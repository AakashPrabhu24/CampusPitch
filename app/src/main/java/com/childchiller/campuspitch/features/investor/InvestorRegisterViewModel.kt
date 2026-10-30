package com.childchiller.campuspitch.features.investor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.childchiller.campuspitch.core.ai.GeminiClientProvider
import com.childchiller.campuspitch.core.ai.InvestorRegistrationAgent
import com.childchiller.campuspitch.core.ai.InvestorRegistrationResult
import com.childchiller.campuspitch.core.models.AgentRunRecord
import com.childchiller.campuspitch.core.models.InvestorProfileRecord
import com.childchiller.campuspitch.core.models.RawInputRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class InvestorRegisterUiState(
    val inputSentence: String = "",
    val isListening: Boolean = false,
    val audioLevel: Float = 0f,
    val isProcessing: Boolean = false,
    val result: InvestorRegistrationResult? = null,
    val isSubmittedPending: Boolean = false,
    val issuedCode: String? = null,
    val errorMessage: String? = null
)

class InvestorRegisterViewModel(
    private val agent: InvestorRegistrationAgent = InvestorRegistrationAgent(
        generativeModel = try {
            GeminiClientProvider.createModel()
        } catch (_: Exception) {
            null
        }
    ),
    private val authGateService: com.childchiller.campuspitch.core.services.AuthGateService = com.childchiller.campuspitch.core.services.AuthGateService(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvestorRegisterUiState())
    val uiState: StateFlow<InvestorRegisterUiState> = _uiState.asStateFlow()

    fun updateInputSentence(text: String) {
        _uiState.update { it.copy(inputSentence = text, errorMessage = null) }
    }

    fun setListeningState(isListening: Boolean, audioLevel: Float = 0f) {
        _uiState.update { it.copy(isListening = isListening, audioLevel = audioLevel) }
    }

    fun processRegistrationUtterance(sentence: String) {
        val trimmed = sentence.trim()
        if (trimmed.isBlank()) return

        _uiState.update { it.copy(isProcessing = true, errorMessage = null) }

        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: "investor_${UUID.randomUUID().toString().take(8)}"
            val res = agent.processRegistrationSentence(trimmed, uid)

            // Log to Data Lake: raw zone and audit zone
            logDataLakeIntake(uid, trimmed, res)

            _uiState.update {
                it.copy(
                    isProcessing = false,
                    result = res,
                    errorMessage = if (res.profile == null) "Could not identify fund details. Please try saying: 'My fund is [Fund Name] and we are an AIF Category I...'" else null
                )
            }
        }
    }

    fun confirmAndSubmit(password: String = "InvestorPassword2026!") {
        val currentProfile = _uiState.value.result?.profile ?: return

        _uiState.update { it.copy(isProcessing = true, errorMessage = null) }

        viewModelScope.launch {
            val fundPrefix = currentProfile.fundName.filter { it.isLetter() }.take(3).uppercase().ifBlank { "BPV" }
            val uniqueSuffix = UUID.randomUUID().toString().replace("-", "").take(6).uppercase()
            val uniqueCode = "CCV-$fundPrefix-$uniqueSuffix"

            val result = authGateService.registerNewInvestor(
                email = currentProfile.officialEmail,
                password = password,
                fundName = currentProfile.fundName,
                aifCategory = currentProfile.aifCategory,
                phone = currentProfile.mobile,
                website = currentProfile.website,
                uniqueCode = uniqueCode
            )

            when (result) {
                is com.childchiller.campuspitch.core.services.AuthResult.Success -> {
                    val code = result.data
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            isSubmittedPending = false,
                            issuedCode = code
                        )
                    }
                }
                is com.childchiller.campuspitch.core.services.AuthResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    private fun logDataLakeIntake(
        uid: String,
        inputSentence: String,
        res: InvestorRegistrationResult
    ) {
        try {
            val rawRecord = RawInputRecord(
                id = UUID.randomUUID().toString(),
                uid = uid,
                inputChannel = "voice_multilingual",
                rawTranscriptRedacted = inputSentence,
                transcriptEnglish = inputSentence,
                languageCode = if (inputSentence.contains("fondo", ignoreCase = true)) "es-ES" else "en-IN",
                languageName = if (inputSentence.contains("fondo", ignoreCase = true)) "Spanish" else "English",
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
                structuredOutput = res.profile?.toMap() ?: emptyMap(),
                fieldConfidence = res.fieldConfidence,
                userConfirmed = false,
                latencyMs = res.executionLatencyMs,
                timestamp = System.currentTimeMillis()
            )
            firestore.collection("agent_runs").document(agentRun.id).set(agentRun.toMap())
        } catch (e: Exception) {
            // Offline resilience
        }
    }
}
