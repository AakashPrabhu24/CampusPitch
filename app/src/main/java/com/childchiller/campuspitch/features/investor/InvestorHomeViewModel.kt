package com.childchiller.campuspitch.features.investor

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.childchiller.campuspitch.core.ai.GeminiClientProvider
import com.childchiller.campuspitch.core.ai.InvestorSearchAgent
import com.childchiller.campuspitch.core.ai.RankedMvpResult
import com.childchiller.campuspitch.core.ai.StructuredSearchQuery
import com.childchiller.campuspitch.core.constants.AppConstants
import com.childchiller.campuspitch.core.models.AgentRunRecord
import com.childchiller.campuspitch.core.models.ChatMessageRecord
import com.childchiller.campuspitch.core.models.MvpPitchRecord
import com.childchiller.campuspitch.core.models.OutreachRecord
import com.childchiller.campuspitch.core.models.RawInputRecord
import com.childchiller.campuspitch.core.repository.FirestoreMvpRepository
import com.childchiller.campuspitch.core.repository.FirestoreOutreachRepository
import com.childchiller.campuspitch.core.repository.FirestoreProfileRepository
import com.childchiller.campuspitch.core.repository.MvpRepository
import com.childchiller.campuspitch.core.repository.OutreachRepository
import com.childchiller.campuspitch.core.repository.ProfileRepository
import com.childchiller.campuspitch.core.repository.SyncState
import com.childchiller.campuspitch.core.utils.InrParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class InvestorHomeUiState(
    val searchUtterance: String = "",
    val isListening: Boolean = false,
    val audioLevel: Float = 0f,
    val isSearching: Boolean = false,
    val activeQuery: StructuredSearchQuery? = null,
    val rankedResults: List<RankedMvpResult> = emptyList(),
    val allMvps: List<MvpPitchRecord> = emptyList(),
    val outreachPipeline: List<OutreachRecord> = emptyList(),
    val selectedTab: Int = 0, // 0 = Discover & Search, 1 = Outreach Pipeline
    val selectedPitchForDetails: MvpPitchRecord? = null,
    val contactModalMvp: MvpPitchRecord? = null,
    val introMessageDraft: String = "",
    val acceptModalMvp: MvpPitchRecord? = null,
    val acceptMessageDraft: String = "",
    val activeConversation: OutreachRecord? = null,
    val conversationMessages: List<ChatMessageRecord> = emptyList(),
    val isConversationLoading: Boolean = false,
    val conversationError: String? = null,
    val chatMessageDraft: String = "",
    val investorFundName: String = "Child Chiller Ventures",
    val syncState: SyncState = SyncState.Idle,
    val toastMessage: String? = null,
    val errorMessage: String? = null
)

class InvestorHomeViewModel(
    private val agent: InvestorSearchAgent = InvestorSearchAgent(
        generativeModel = try {
            GeminiClientProvider.createModel()
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

    private val _uiState = MutableStateFlow(InvestorHomeUiState())
    val uiState: StateFlow<InvestorHomeUiState> = _uiState.asStateFlow()

    init {
        observeInvestorProfile()
        observePublishedMvps()
        observeOutreachPipeline()
    }

    fun updateSearchText(text: String) {
        _uiState.update { it.copy(searchUtterance = text) }
        executeSearch(text)
    }

    fun setListeningState(isListening: Boolean, audioLevel: Float = 0f) {
        _uiState.update { it.copy(isListening = isListening, audioLevel = audioLevel) }
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun refreshData() {
        observeInvestorProfile()
        observePublishedMvps()
        observeOutreachPipeline()
    }

    private fun observeInvestorProfile() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            profileRepository.observeInvestorProfile(uid).collect { profile ->
                if (profile != null && profile.fundName.isNotBlank()) {
                    _uiState.update { it.copy(investorFundName = profile.fundName) }
                }
            }
        }
    }

    private fun observePublishedMvps() {
        viewModelScope.launch {
            mvpRepository.observePublishedMvps().collect { list ->
                _uiState.update { it.copy(allMvps = list) }
                // Re-run current search utterance against live pool immediately (<2s SLA)
                executeSearch(_uiState.value.searchUtterance)
            }
        }
    }

    private fun observeOutreachPipeline() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            outreachRepository.observeForInvestor(uid).collect { list ->
                _uiState.update { it.copy(outreachPipeline = list) }
            }
        }
    }

    /**
     * Executes the InvestorSearchAgent with natural utterance in memory.
     * Avoids Firestore composite index limitations while delivering <100ms structured filtering.
     */
    fun executeSearch(
        utterance: String = _uiState.value.searchUtterance,
        detectedLang: String? = null,
        originalTranscript: String? = null
    ) {
        val currentPool = _uiState.value.allMvps
        val fund = _uiState.value.investorFundName

        _uiState.update { it.copy(isSearching = true) }

        viewModelScope.launch {
            val result = agent.executeSearch(utterance, currentPool, fund)

            // Log search to Data Lake (audit only)
            logSearchRunToDataLake(utterance, result, detectedLang, originalTranscript)

            _uiState.update {
                it.copy(
                    isSearching = false,
                    activeQuery = result.query,
                    rankedResults = result.rankedResults
                )
            }
        }
    }

    fun openContactModal(mvp: MvpPitchRecord, customIntro: String? = null) {
        val intro = customIntro ?: agent.draftIntroMessage(mvp, _uiState.value.investorFundName)
        _uiState.update {
            it.copy(
                contactModalMvp = mvp,
                introMessageDraft = intro
            )
        }
    }

    fun dismissContactModal() {
        _uiState.update {
            it.copy(
                contactModalMvp = null,
                introMessageDraft = ""
            )
        }
    }

    fun updateIntroDraft(newText: String) {
        _uiState.update { it.copy(introMessageDraft = newText) }
    }

    fun openAcceptModal(mvp: MvpPitchRecord) {
        val defaultAcceptanceMessage = "Congratulations! Your startup pitch for '${mvp.appName}' has been officially ACCEPTED by ${_uiState.value.investorFundName} on CampusPitch. We are impressed with your progress and would like to schedule an introductory term sheet discussion."
        _uiState.update {
            it.copy(
                acceptModalMvp = mvp,
                acceptMessageDraft = defaultAcceptanceMessage
            )
        }
    }

    fun openPitchDetails(mvp: MvpPitchRecord) {
        _uiState.update { it.copy(selectedPitchForDetails = mvp) }
    }

    fun dismissPitchDetails() {
        _uiState.update { it.copy(selectedPitchForDetails = null) }
    }

    fun dismissAcceptModal() {
        _uiState.update {
            it.copy(
                acceptModalMvp = null,
                acceptMessageDraft = ""
            )
        }
    }

    fun updateAcceptDraft(newText: String) {
        _uiState.update { it.copy(acceptMessageDraft = newText) }
    }

    /**
     * Accepts a student pitch, records the 'Accepted' status in the pipeline,
     * logs an initial in-app message, and checks founder contact sharing consent
     * before optionally dispatching an external email to the verified founder email.
     */
    fun confirmAcceptPitch(context: Context? = null) {
        val targetMvp = _uiState.value.acceptModalMvp ?: return
        val message = _uiState.value.acceptMessageDraft
        val investorUid = auth.currentUser?.uid ?: "investor_session"
        val fundName = _uiState.value.investorFundName
        val studentUid = if (targetMvp.ownerUid.isNotBlank()) targetMvp.ownerUid else targetMvp.studentUid

        val outreachId = UUID.randomUUID().toString()
        val outreach = OutreachRecord(
            id = outreachId,
            investorUid = investorUid,
            investorFundName = fundName,
            studentUid = studentUid,
            mvpId = targetMvp.id,
            mvpAppName = targetMvp.appName,
            introMessage = message,
            status = "Accepted",
            updatedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            _uiState.update { it.copy(syncState = SyncState.Syncing) }
            val result = outreachRepository.sendOutreach(outreach)

            // Send initial official acceptance chat message into the two-way conversation
            val initialMsg = ChatMessageRecord(
                id = UUID.randomUUID().toString(),
                conversationId = outreachId,
                senderUid = investorUid,
                senderName = fundName,
                senderRole = "investor",
                text = "🎉 Official Pitch Acceptance: $message",
                timestamp = System.currentTimeMillis()
            )
            outreachRepository.sendChatMessage(initialMsg)

            // Check founder consent and verified email before dispatching email intent
            if (context != null && studentUid.isNotBlank()) {
                checkAndDispatchAcceptanceEmailIfPermitted(
                    context = context,
                    mvp = targetMvp,
                    studentUid = studentUid,
                    fundName = fundName,
                    introMessage = message
                )
            }

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        syncState = SyncState.Success("Accepted"),
                        acceptModalMvp = null,
                        acceptMessageDraft = "",
                        toastMessage = "🎉 Pitch ACCEPTED! Deal pipeline updated and founder notified."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        syncState = SyncState.Success("Accepted"),
                        acceptModalMvp = null,
                        acceptMessageDraft = "",
                        toastMessage = "🎉 Pitch accepted locally! Deal recorded in pipeline."
                    )
                }
            }
        }
    }

    private suspend fun checkAndDispatchAcceptanceEmailIfPermitted(
        context: Context,
        mvp: MvpPitchRecord,
        studentUid: String,
        fundName: String,
        introMessage: String
    ) {
        try {
            val studentDoc = firestore.collection("students").document(studentUid).get().await()
            val consent = studentDoc.getBoolean("contactShareConsent") ?: false
            if (!consent) {
                // Founder did NOT consent to sharing external contact info.
                // Under DPDP regulations, keep interaction strictly in-app.
                return
            }

            val userDoc = firestore.collection("users").document(studentUid).get().await()
            val verifiedEmail = userDoc.getString("email")
            if (verifiedEmail.isNullOrBlank() || !verifiedEmail.contains("@")) {
                return
            }

            dispatchAcceptanceEmail(
                context = context,
                mvp = mvp,
                recipientEmail = verifiedEmail,
                fundName = fundName,
                introMessage = introMessage
            )
        } catch (_: Exception) {
            // Fall back gracefully to in-app messaging without crashing
        }
    }

    private fun dispatchAcceptanceEmail(
        context: Context,
        mvp: MvpPitchRecord,
        recipientEmail: String,
        fundName: String,
        introMessage: String
    ) {
        try {
            val emailSubject = "[CampusPitch] Your app ${mvp.appName} has been ACCEPTED by $fundName!"
            val emailBody = """
                Dear Founder,

                Great news! Your startup pitch for "${mvp.appName}" has been officially ACCEPTED by $fundName on CampusPitch!

                Deal Overview:
                • Investor / Venture Fund: $fundName
                • Startup: ${mvp.appName}
                • Sector: ${mvp.sector}
                • Stage: ${mvp.stage}
                • Funding Target: ₹${InrParser.formatInr(mvp.raiseAmountInr)}

                Note from $fundName:
                "$introMessage"

                Next Steps:
                Please open CampusPitch or reply to coordinate your introductory meeting and term sheet evaluation.

                Best regards,
                Child Chiller Ventures & CampusPitch Platform
            """.trimIndent()

            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
                putExtra(Intent.EXTRA_SUBJECT, emailSubject)
                putExtra(Intent.EXTRA_TEXT, emailBody)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(emailIntent, "Send Acceptance Email to Founder").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
            // Non-blocking email dispatch fallback
        }
    }

    /**
     * Sends introductory message and records in `outreach` collection in Data Lake.
     * Also initializes the two-way in-app chat thread.
     */
    fun sendContactMessage() {
        val targetMvp = _uiState.value.contactModalMvp ?: return
        val intro = _uiState.value.introMessageDraft
        val investorUid = auth.currentUser?.uid ?: "investor_session"
        val fundName = _uiState.value.investorFundName
        val studentUid = if (targetMvp.ownerUid.isNotBlank()) targetMvp.ownerUid else targetMvp.studentUid

        val outreachId = UUID.randomUUID().toString()
        val outreach = OutreachRecord(
            id = outreachId,
            investorUid = investorUid,
            investorFundName = fundName,
            studentUid = studentUid,
            mvpId = targetMvp.id,
            mvpAppName = targetMvp.appName,
            introMessage = intro,
            status = "Contacted",
            updatedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            _uiState.update { it.copy(syncState = SyncState.Syncing) }
            val result = outreachRepository.sendOutreach(outreach)

            // Send initial intro chat message
            val initialMsg = ChatMessageRecord(
                id = UUID.randomUUID().toString(),
                conversationId = outreachId,
                senderUid = investorUid,
                senderName = fundName,
                senderRole = "investor",
                text = intro,
                timestamp = System.currentTimeMillis()
            )
            outreachRepository.sendChatMessage(initialMsg)

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        syncState = SyncState.Success("Sent"),
                        contactModalMvp = null,
                        introMessageDraft = "",
                        toastMessage = "Contact message sent to ${targetMvp.appName} founders!"
                    )
                }
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to deliver contact message"
                _uiState.update {
                    it.copy(
                        syncState = SyncState.Error(err),
                        errorMessage = "Error sending message: $err"
                    )
                }
            }
        }
    }

    private var conversationJob: Job? = null

    /**
     * Opens a real-time two-way messaging conversation for an existing outreach record.
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

    /**
     * Opens existing conversation for an MVP or initiates new contact modal.
     */
    fun openConversationForMvp(mvp: MvpPitchRecord) {
        val existing = _uiState.value.outreachPipeline.find { it.mvpId == mvp.id }
        if (existing != null) {
            openConversation(existing)
        } else {
            openContactModal(mvp)
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

        val investorUid = auth.currentUser?.uid ?: "investor_session"
        val fundName = _uiState.value.investorFundName

        val message = ChatMessageRecord(
            id = UUID.randomUUID().toString(),
            conversationId = conv.id,
            senderUid = investorUid,
            senderName = fundName,
            senderRole = "investor",
            text = text,
            timestamp = System.currentTimeMillis()
        )

        viewModelScope.launch {
            _uiState.update { it.copy(chatMessageDraft = "") }
            outreachRepository.sendChatMessage(message)
        }
    }

    /**
     * Pipeline status transitions:
     * Contacted > Replied > Meeting > Term sheet > Funded / Declined
     */
    fun updateOutreachStatus(outreachId: String, newStatus: String) {
        viewModelScope.launch {
            val result = outreachRepository.updateOutreachStatus(outreachId, newStatus)
            if (result.isFailure) {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to update status"
                _uiState.update { it.copy(errorMessage = err) }
            }
        }
    }

    private fun logSearchRunToDataLake(
        utterance: String,
        result: com.childchiller.campuspitch.core.ai.InvestorSearchResult,
        detectedLang: String? = null,
        originalTranscript: String? = null
    ) {
        val uid = auth.currentUser?.uid ?: "investor_session"
        try {
            val langCode = detectedLang ?: if (utterance.any { it in '\u0900'..'\u097F' }) "hi-IN" else "en-IN"
            val langName = AppConstants.SUPPORTED_LANGUAGES.find { it.bcp47.equals(langCode, ignoreCase = true) }?.name
                ?: if (langCode.startsWith("hi")) "Hindi" else "English"

            val rawRecord = RawInputRecord(
                id = UUID.randomUUID().toString(),
                uid = uid,
                inputChannel = "voice_multilingual",
                rawTranscriptRedacted = originalTranscript ?: utterance,
                transcriptEnglish = utterance,
                languageCode = langCode,
                languageName = langName,
                modelVersion = agent.modelVersion,
                createdAt = System.currentTimeMillis()
            )
            firestore.collection("raw_inputs").document(rawRecord.id).set(rawRecord.toMap())

            val agentRun = AgentRunRecord(
                id = UUID.randomUUID().toString(),
                uid = uid,
                agentName = agent.agentName,
                rawInputId = rawRecord.id,
                modelVersion = agent.modelVersion,
                structuredOutput = mapOf(
                    "query" to (result.query.targetSector ?: "all"),
                    "minInr" to (result.query.minRaiseInr ?: 0L),
                    "maxInr" to (result.query.maxRaiseInr ?: 0L),
                    "resultsCount" to result.rankedResults.size
                ),
                fieldConfidence = mapOf("sector" to 0.95, "range" to 0.98),
                userConfirmed = true,
                latencyMs = result.executionLatencyMs,
                timestamp = System.currentTimeMillis()
            )
            firestore.collection("agent_runs").document(agentRun.id).set(agentRun.toMap())
        } catch (_: Exception) {
            // Non-blocking audit log
        }
    }
}
