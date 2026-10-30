package com.childchiller.campuspitch.features.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.childchiller.campuspitch.core.models.InvestorProfileRecord
import com.childchiller.campuspitch.core.repository.FirestoreProfileRepository
import com.childchiller.campuspitch.core.repository.ProfileRepository
import com.childchiller.campuspitch.core.services.AuthGateService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class AdminApprovalUiState(
    val pendingInvestors: List<InvestorProfileRecord> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null
)

class AdminApprovalViewModel(
    private val profileRepository: ProfileRepository = FirestoreProfileRepository(),
    private val authGateService: AuthGateService = AuthGateService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminApprovalUiState())
    val uiState: StateFlow<AdminApprovalUiState> = _uiState.asStateFlow()

    init {
        observePendingInvestors()
    }

    private fun observePendingInvestors() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            profileRepository.observePendingInvestors().collect { list ->
                _uiState.update {
                    it.copy(
                        pendingInvestors = list,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun approveInvestor(investor: InvestorProfileRecord) {
        val codeSuffix = UUID.randomUUID().toString().take(4).uppercase()
        val fundPrefix = investor.fundName.filter { it.isLetter() }.take(3).uppercase().ifBlank { "BPV" }
        val generatedCode = "CCV-$fundPrefix-$codeSuffix"

        viewModelScope.launch {
            val result = profileRepository.approveInvestor(investor.uid, generatedCode)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        message = "Approved ${investor.fundName}! Issued code: $generatedCode",
                        errorMessage = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        errorMessage = "Approval failed: ${result.exceptionOrNull()?.localizedMessage}",
                        message = null
                    )
                }
            }
        }
    }

    fun rejectInvestor(investor: InvestorProfileRecord) {
        viewModelScope.launch {
            val result = profileRepository.rejectInvestor(investor.uid)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        message = "Rejected ${investor.fundName}.",
                        errorMessage = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        errorMessage = "Rejection failed: ${result.exceptionOrNull()?.localizedMessage}",
                        message = null
                    )
                }
            }
        }
    }

    fun signOut(onSignedOut: () -> Unit) {
        authGateService.signOut()
        onSignedOut()
    }

    fun clearMessages() {
        _uiState.update { it.copy(message = null, errorMessage = null) }
    }
}