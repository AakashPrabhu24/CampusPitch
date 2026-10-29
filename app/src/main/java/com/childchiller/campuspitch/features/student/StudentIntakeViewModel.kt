package com.childchiller.campuspitch.features.student

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.childchiller.campuspitch.core.ai.DetectedLanguageInfo
import com.childchiller.campuspitch.core.ai.GeminiClientProvider
import com.childchiller.campuspitch.core.ai.MultiLanguageAudioResult
import com.childchiller.campuspitch.core.ai.MultiLanguageVoiceAgent
import com.childchiller.campuspitch.core.ai.StudentIntakeAgent
import com.childchiller.campuspitch.core.ai.StudentIntakeResult
import com.childchiller.campuspitch.core.constants.AppConstants
import com.childchiller.campuspitch.core.models.AgentRunRecord
import com.childchiller.campuspitch.core.models.ChatMessageRecord
import com.childchiller.campuspitch.core.models.MvpPitchRecord
import com.childchiller.campuspitch.core.models.OutreachRecord
import com.childchiller.campuspitch.core.models.RawInputRecord
import com.childchiller.campuspitch.core.models.StudentProfileRecord
import kotlinx.coroutines.Job
import java.util.UUID
import com.childchiller.campuspitch.core.repository.FirestoreMvpRepository
import com.childchiller.campuspitch.core.repository.FirestoreOutreachRepository
import com.childchiller.campuspitch.core.repository.FirestoreProfileRepository
import com.childchiller.campuspitch.core.repository.MvpRepository
import com.childchiller.campuspitch.core.repository.OutreachRepository
import com.childchiller.campuspitch.core.repository.ProfileRepository
import com.childchiller.campuspitch.core.repository.SyncState
import com.childchiller.campuspitch.core.services.GitHubRepoService
import com.childchiller.campuspitch.core.services.SpeechRecognizerHelper
import com.childchiller.campuspitch.core.utils.AadhaarCrypto
import com.childchiller.campuspitch.core.utils.InrParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class IntakeConversationState {
    IDLE,
    PROCESSING,
    AWAITING_FOLLOW_UP,
    READY_FOR_CONFIRMATION,
    CONFIRMED_SUCCESS
}

data class StudentIntakeUiState(
    val conversationState: IntakeConversationState = IntakeConversationState.IDLE,
    val currentSentence: String = "",
    val isListening: Boolean = false,
    val audioLevel: Float = 0f,
    val activeDraftMvp: MvpPitchRecord? = null,
    val detectedLanguage: DetectedLanguageInfo = DetectedLanguageInfo(),
    val transcriptOriginal: String = "",
    val transcriptEnglish: String = "",
    val followUpQuestion: String? = null,
    val followUpOriginal: String? = null,
    val followUpEnglish: String? = null,
    val missingField: String? = null,
    val fieldConfidence: Map<String, Double> = emptyMap(),
    val studentProfile: StudentProfileRecord? = null,
    val uploadedMvps: List<MvpPitchRecord> = emptyList(),
    val incomingOutreach: List<OutreachRecord> = emptyList(),
    val activeConversation: OutreachRecord? = null,
    val conversationMessages: List<ChatMessageRecord> = emptyList(),
    val isConversationLoading: Boolean = false,
    val conversationError: String? = null,
    val chatMessageDraft: String = "",
    val syncState: SyncState = SyncState.Idle,
    val attachedFileName: String? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

class StudentIntakeViewModel(
    private val agent: StudentIntakeAgent = StudentIntakeAgent(
        generativeModel = try {
            GeminiClientProvider.createModel()
        } catch (_: Exception) {
            null
        }
    ),
    val voiceAgent: MultiLanguageVoiceAgent = MultiLanguageVoiceAgent(
        generativeModel = try {
            GeminiClientProvider.createAudioModel(systemInstruction = MultiLanguageVoiceAgent.SYSTEM_INSTRUCTION)
        } catch (_: Exception) {
            null
        }
    ),
    private val mvpRepository: MvpRepository = FirestoreMvpRepository(),
    private val outreachRepository: OutreachRepository = FirestoreOutreachRepository(),
    private val profileRepository: ProfileRepository = FirestoreProfileRepository(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentIntakeUiState())
    val uiState: StateFlow<StudentIntakeUiState> = _uiState.asStateFlow()

    private var speechHelper: SpeechRecognizerHelper? = null

    init {
        observeStudentData()
    }

    private fun observeStudentData() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            profileRepository.observeStudentProfile(uid).collect { profile ->
                _uiState.update { it.copy(studentProfile = profile) }
            }
        }
        viewModelScope.launch {
            mvpRepository.observeStudentMvps(uid).collect { list ->
                _uiState.update { it.copy(uploadedMvps = list) }
            }
        }
        viewModelScope.launch {
            outreachRepository.observeForStudent(uid).collect { list ->
                _uiState.update { it.copy(incomingOutreach = list) }
            }
        }
    }

    fun initializeSpeech(context: Context) {
        if (speechHelper == null) {
            speechHelper = SpeechRecognizerHelper(context, voiceAgent)
            viewModelScope.launch {
                speechHelper?.isListening?.collect { listening ->
                    _uiState.update { it.copy(isListening = listening) }
                }
            }
            viewModelScope.launch {
                speechHelper?.partialTranscript?.collect { text ->
                    if (text.isNotBlank()) {
                        _uiState.update { it.copy(currentSentence = text) }
                    }
                }
            }
            viewModelScope.launch {
                speechHelper?.rmsDb?.collect { level ->
                    _uiState.update { it.copy(audioLevel = level) }
                }
            }
            viewModelScope.launch {
                speechHelper?.error?.collect { err ->
                    if (err != null) {
                        _uiState.update { it.copy(errorMessage = err) }
                    }
                }
            }
            viewModelScope.launch {
                speechHelper?.lastResult?.collect { res ->
                    if (res != null) {
                        handleMultiLanguageResult(res)
                    }
                }
            }
        }
    }

    fun onSentenceChange(text: String) {
        _uiState.update { it.copy(currentSentence = text, errorMessage = null) }
    }

    fun toggleVoiceInput() {
        val helper = speechHelper ?: return
        if (_uiState.value.isListening) {
            helper.stopListening()
            if (_uiState.value.currentSentence.isNotBlank()) {
                submitNaturalSentence(_uiState.value.currentSentence)
            }
        } else {
            _uiState.update { it.copy(errorMessage = null) }
            val forcedHint = if (_uiState.value.activeDraftMvp != null) "student_edit" else "student_upload"
            helper.startListening(forcedHint) { result ->
                handleMultiLanguageResult(result)
            }
        }
    }

    /**
     *
     * Takes unconstrained spoken/typed sentence in ANY language,
     * auto-detects language, translates to English, and populates structured draft.
     */
    fun submitNaturalSentence(sentence: String) {
        val trimmed = sentence.trim()
        if (trimmed.isBlank()) return

        val studentUid = auth.currentUser?.uid ?: "student_preview_user"

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    conversationState = IntakeConversationState.PROCESSING,
                    errorMessage = null
                )
            }

            // 1. Check for GitHub repository URL import
            val gitHubCoords = GitHubRepoService.parseGitHubUrl(trimmed)
            if (gitHubCoords != null) {
                val (owner, repo) = gitHubCoords
                val repoInfoResult = GitHubRepoService.fetchPublicRepo(owner, repo)
                val repoInfo = repoInfoResult.getOrNull()
                if (repoInfo != null) {
                    val draft = GitHubRepoService.createDraftMvpFromGitHub(repoInfo, studentUid)
                    _uiState.update {
                        it.copy(
                            conversationState = IntakeConversationState.READY_FOR_CONFIRMATION,
                            activeDraftMvp = draft,
                            currentSentence = "",
                            statusMessage = "Imported public repository from GitHub!"
                        )
                    }
                    return@launch
                }
            }

            // 2. Delegate to Multilingual Voice & Intent Agent
            val forcedHint = if (_uiState.value.activeDraftMvp != null) "student_edit" else "student_upload"
            val multiResult = voiceAgent.processText(trimmed, forcedHint)
            handleMultiLanguageResult(multiResult)
        }
    }

    private fun handleMultiLanguageResult(result: MultiLanguageAudioResult) {
        val studentUid = auth.currentUser?.uid ?: "student_preview_user"
        val fields = result.fields

        val existingDraft = _uiState.value.activeDraftMvp

        val appName = (fields["appName"] as? String)?.takeIf { it.isNotBlank() && !it.equals("MyStartup", ignoreCase = true) }
            ?: existingDraft?.appName
            ?: ""

        val description = (fields["description"] as? String)?.takeIf { it.isNotBlank() }
            ?: if (result.transcriptEnglish.isNotBlank()) result.transcriptEnglish else existingDraft?.description ?: ""

        val sector = (fields["sector"] as? String)?.takeIf { it.isNotBlank() }
            ?: existingDraft?.sector
            ?: "AI, SaaS & DevTools"

        val stage = (fields["stage"] as? String) ?: existingDraft?.stage ?: "MVP"

        val rawAmount = fields["raiseAmountInr"]
        val raiseInr = when (rawAmount) {
            is Number -> rawAmount.toLong()
            is String -> InrParser.parse(rawAmount).singleAmountInr ?: 0L
            else -> InrParser.parse(result.transcriptOriginal).singleAmountInr ?: existingDraft?.raiseAmountInr ?: 0L
        }

        val raiseWording = (fields["raiseOriginalWording"] as? String)?.takeIf { it.isNotBlank() }
            ?: InrParser.parse(result.transcriptOriginal).originalWording.ifBlank {
                existingDraft?.raiseOriginalWording ?: if (raiseInr > 0L) "₹$raiseInr" else ""
            }

        val draft = MvpPitchRecord(
            id = existingDraft?.id ?: UUID.randomUUID().toString(),
            ownerUid = studentUid,
            studentUid = studentUid,
            appName = appName,
            description = description,
            sector = sector,
            stage = stage,
            raiseAmountInr = raiseInr,
            raiseOriginalWording = raiseWording,
            oneLineAiSummary = "Campus MVP in $sector: $description",
            updatedAt = System.currentTimeMillis()
        )

        val essentialMissing = appName.isBlank() || description.isBlank() || raiseInr == 0L
        val conversationState = if (essentialMissing && result.followUpOriginal != null) {
            IntakeConversationState.AWAITING_FOLLOW_UP
        } else {
            IntakeConversationState.READY_FOR_CONFIRMATION
        }

        _uiState.update {
            it.copy(
                conversationState = conversationState,
                activeDraftMvp = draft,
                detectedLanguage = result.detectedLanguage,
                transcriptOriginal = if (result.transcriptOriginal.isNotBlank()) result.transcriptOriginal else it.currentSentence,
                transcriptEnglish = if (result.transcriptEnglish.isNotBlank()) result.transcriptEnglish else it.currentSentence,
                followUpQuestion = result.followUpOriginal ?: result.followUpEnglish,
                followUpOriginal = result.followUpOriginal,
                followUpEnglish = result.followUpEnglish,
                missingField = result.missingFields.firstOrNull(),
                currentSentence = "",
                statusMessage = "Detected ${result.detectedLanguage.name} (${result.detectedLanguage.bcp47})"
            )
        }
    }

    /**
     * User language override from confirmation chip.
     */
    fun overrideLanguage(override: DetectedLanguageInfo) {
        _uiState.update { it.copy(detectedLanguage = override) }
        speechHelper?.overrideDetectedLanguage(override)
    }

    /**
     * User confirmation step before writing to the Data Lake.
     */
    fun confirmAndUploadMvp() {
        val draft = _uiState.value.activeDraftMvp ?: return
        val studentUid = auth.currentUser?.uid ?: "student_preview_user"
        val profile = _uiState.value.studentProfile

        val published = draft.copy(
            ownerUid = studentUid,
            studentUid = studentUid,
            ownerName = profile?.studentName ?: "Campus Founder",
            ownerCollege = profile?.collegeName ?: "Campus Pitch Network",
            status = "published",
            updatedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    conversationState = IntakeConversationState.PROCESSING,
                    syncState = SyncState.Syncing,
                    errorMessage = null
                )
            }

            try {
                // 1. Raw Zone audit logging with original transcript, English translation, language code & model
                val origTranscript = if (_uiState.value.transcriptOriginal.isNotBlank()) {
                    _uiState.value.transcriptOriginal
                } else {
                    draft.description
                }

                val engTranscript = if (_uiState.value.transcriptEnglish.isNotBlank()) {
                    _uiState.value.transcriptEnglish
                } else {
                    draft.description
                }

                val rawInput = RawInputRecord(
                    id = UUID.randomUUID().toString(),
                    uid = studentUid,
                    inputChannel = "voice_multilingual",
                    rawTranscriptRedacted = AadhaarCrypto.redactAadhaarFromTranscript(origTranscript),
                    transcriptEnglish = engTranscript,
                    languageCode = _uiState.value.detectedLanguage.bcp47,
                    languageName = _uiState.value.detectedLanguage.name,
                    modelVersion = voiceAgent.modelVersion
                )
                firestore.collection("raw_inputs").document(rawInput.id).set(rawInput.toMap())
            } catch (_: Exception) {
                // Non-blocking for audit log
            }

            // 2. Curated Zone - real Firestore repository write (curated in English for universal discoverability)
            val result = mvpRepository.saveMvp(published)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        conversationState = IntakeConversationState.CONFIRMED_SUCCESS,
                        syncState = SyncState.Success("Saved"),
                        activeDraftMvp = null,
                        attachedFileName = null,
                        statusMessage = "${published.appName} published to Central Data Lake!"
                    )
                }
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to save to server"
                _uiState.update {
                    it.copy(
                        conversationState = IntakeConversationState.IDLE,
                        syncState = SyncState.Error(err),
                        errorMessage = "Save failed: $err",
                        statusMessage = null
                    )
                }
            }
        }
    }

    fun attachFileName(name: String) {
        _uiState.update { it.copy(attachedFileName = name) }
    }

    fun cancelDraft() {
        _uiState.update {
            it.copy(
                conversationState = IntakeConversationState.IDLE,
                activeDraftMvp = null,
                followUpQuestion = null,
                currentSentence = "",
                statusMessage = null
            )
        }
    }

    fun updateMvpRaise(mvpId: String, newRaiseText: String) {
        val inrResult = InrParser.parse(newRaiseText)
        val amount = inrResult.singleAmountInr ?: inrResult.minInr ?: 0L
        val currentMvp = _uiState.value.uploadedMvps.find { it.id == mvpId } ?: return

        val updated = currentMvp.copy(
            raiseAmountInr = if (amount > 0L) amount else currentMvp.raiseAmountInr,
            raiseOriginalWording = newRaiseText,
            updatedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            _uiState.update { it.copy(syncState = SyncState.Syncing) }
            val result = mvpRepository.saveMvp(updated)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        syncState = SyncState.Success("Saved"),
                        statusMessage = "Raise updated to $newRaiseText"
                    )
                }
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Update failed"
                _uiState.update {
                    it.copy(
                        syncState = SyncState.Error(err),
                        errorMessage = err
                    )
                }
            }
        }
    }

    fun deleteMvp(mvpId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(syncState = SyncState.Syncing) }
            val result = mvpRepository.deleteMvp(mvpId)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        syncState = SyncState.Success("Deleted"),
                        statusMessage = "App removed from Central Data Lake."
                    )
                }
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Delete failed"
                _uiState.update {
                    it.copy(
                        syncState = SyncState.Error(err),
                        errorMessage = "Delete error: $err"
                    )
                }
            }
        }
    }

    fun updateOutreachStatus(outreachId: String, newStatus: String) {
        viewModelScope.launch {
            val result = outreachRepository.updateOutreachStatus(outreachId, newStatus)
            if (result.isFailure) {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Update failed"
                _uiState.update { it.copy(errorMessage = err) }
            }
        }
    }

    private var conversationJob: Job? = null

    /**
     * Opens a real-time two-way messaging conversation for an investor outreach record.
     */
    fun openConversation(outreach: OutreachRecord) {
        conversationJob?.cancel()
        _uiState.update {
            it.copy(
                activeConversation = outreach,
                conversationMessages = emptyList(),
                isConversationLoading = true,
                conversationError = null,
                chatMessageDraft = ""
            )
        }
        conversationJob = viewModelScope.launch {
            try {
                outreachRepository.observeMessages(outreach.id).collect { msgs ->
                    _uiState.update {
                        it.copy(
                            conversationMessages = msgs,
                            isConversationLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isConversationLoading = false,
                        conversationError = e.localizedMessage ?: "Failed to load messages"
                    )
                }
            }
        }
    }

    fun closeConversation() {
        conversationJob?.cancel()
        conversationJob = null
        _uiState.update {
            it.copy(
                activeConversation = null,
                conversationMessages = emptyList(),
                isConversationLoading = false,
                conversationError = null,
                chatMessageDraft = ""
            )
        }
    }

    fun updateChatDraft(newText: String) {
        _uiState.update { it.copy(chatMessageDraft = newText) }
    }

    fun sendConversationMessage() {
        val conv = _uiState.value.activeConversation ?: return
        val text = _uiState.value.chatMessageDraft.trim()
        if (text.isBlank()) return

        val studentUid = auth.currentUser?.uid ?: "student_preview_user"
        val studentName = _uiState.value.studentProfile?.studentName ?: "Founder"

        val message = ChatMessageRecord(
            id = UUID.randomUUID().toString(),
            conversationId = conv.id,
            senderUid = studentUid,
            senderName = studentName,
            senderRole = "student",
            text = text,
            timestamp = System.currentTimeMillis()
        )

        viewModelScope.launch {
            _uiState.update { it.copy(chatMessageDraft = "") }
            outreachRepository.sendChatMessage(message)
            if (conv.status.equals("Contacted", ignoreCase = true) || conv.status.equals("Accepted", ignoreCase = true)) {
                outreachRepository.updateOutreachStatus(conv.id, "Replied")
            }
        }
    }

    fun refreshStudentData() {
        observeStudentData()
    }

    override fun onCleared() {
        super.onCleared()
        speechHelper?.destroy()
        speechHelper = null
    }
}
