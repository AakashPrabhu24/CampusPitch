package com.childchiller.campuspitch.features.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.childchiller.campuspitch.core.constants.AppConstants
import com.childchiller.campuspitch.core.services.AuthGateService
import com.childchiller.campuspitch.core.services.AuthResult
import com.childchiller.campuspitch.core.services.SpeechRecognizerHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class UserRoleSelection {
    STUDENT,
    INVESTOR,
    ADMIN
}

enum class StudentIdentityType {
    COLLEGE_ID,
    AADHAAR
}

enum class AuthMode {
    SIGN_IN,
    SIGN_UP
}

data class RoleGateUiState(
    val selectedRole: UserRoleSelection = UserRoleSelection.STUDENT,
    val authMode: AuthMode = AuthMode.SIGN_IN,
    val emailInput: String = "",
    val passwordInput: String = "",
    val studentIdentityType: StudentIdentityType = StudentIdentityType.COLLEGE_ID,
    val studentIdentityInput: String = "",
    val investorCodeInput: String = "",
    val isListening: Boolean = false,
    val speechTranscript: String = "",
    val audioLevel: Float = 0f,
    val isLoading: Boolean = false,
    val infoMessage: String? = null,
    val errorMessage: String? = null,
    val successNavigationTarget: String? = null
)

class RoleGateViewModel(
    private val authGateService: AuthGateService = AuthGateService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoleGateUiState())
    val uiState: StateFlow<RoleGateUiState> = _uiState.asStateFlow()

    private var speechHelper: SpeechRecognizerHelper? = null

    fun initializeSpeech(context: Context) {
        if (speechHelper == null) {
            speechHelper = SpeechRecognizerHelper(context)
            viewModelScope.launch {
                speechHelper?.isListening?.collect { listening ->
                    _uiState.update { it.copy(isListening = listening) }
                }
            }
            viewModelScope.launch {
                speechHelper?.partialTranscript?.collect { text ->
                    if (text.isNotBlank()) {
                        handleSpokenInput(text)
                    }
                }
            }
            viewModelScope.launch {
                speechHelper?.lastResult?.collect { res ->
                    if (res != null) {
                        val text = res.transcriptEnglish.ifBlank { res.transcriptOriginal }
                        if (text.isNotBlank()) {
                            handleSpokenInput(text)
                        }
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
        }
    }

    fun selectRole(role: UserRoleSelection) {
        _uiState.update {
            it.copy(
                selectedRole = role,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun toggleAuthMode() {
        _uiState.update {
            it.copy(
                authMode = if (it.authMode == AuthMode.SIGN_IN) AuthMode.SIGN_UP else AuthMode.SIGN_IN,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(emailInput = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(passwordInput = value, errorMessage = null) }
    }

    fun selectStudentIdentityType(type: StudentIdentityType) {
        _uiState.update {
            it.copy(
                studentIdentityType = type,
                studentIdentityInput = "",
                errorMessage = null
            )
        }
    }

    fun onStudentIdentityChange(value: String) {
        _uiState.update { it.copy(studentIdentityInput = value, errorMessage = null) }
    }

    fun onInvestorCodeChange(value: String) {
        _uiState.update { it.copy(investorCodeInput = value, errorMessage = null) }
    }

    fun fillDemoInvestorCredentials() {
        _uiState.update {
            it.copy(
                selectedRole = UserRoleSelection.INVESTOR,
                authMode = AuthMode.SIGN_IN,
                emailInput = AppConstants.SEEDED_DEMO_EMAIL,
                passwordInput = AppConstants.SEEDED_DEMO_PASSWORD,
                investorCodeInput = AppConstants.SEEDED_DEMO_INVESTOR_CODE,
                errorMessage = null
            )
        }
    }

    fun fillAdminCredentials() {
        _uiState.update {
            it.copy(
                selectedRole = UserRoleSelection.ADMIN,
                authMode = AuthMode.SIGN_IN,
                emailInput = AppConstants.SEEDED_ADMIN_EMAIL,
                passwordInput = AppConstants.SEEDED_ADMIN_PASSWORD,
                errorMessage = null
            )
        }
    }

    fun toggleVoiceInput() {
        val helper = speechHelper ?: return
        if (_uiState.value.isListening) {
            helper.stopListening()
        } else {
            _uiState.update { it.copy(errorMessage = null) }
            helper.startListening()
        }
    }

    private fun handleSpokenInput(text: String) {
        _uiState.update { it.copy(speechTranscript = text) }
        when (_uiState.value.selectedRole) {
            UserRoleSelection.STUDENT -> {
                _uiState.update { it.copy(studentIdentityInput = text) }
            }
            UserRoleSelection.INVESTOR -> {
                val codeCleaned = text.replace(" ", "").uppercase()
                _uiState.update { it.copy(investorCodeInput = codeCleaned) }
            }
            UserRoleSelection.ADMIN -> {}
        }
    }

    fun submitAuth() {
        val state = _uiState.value
        val email = state.emailInput.trim()
        val password = state.passwordInput.trim()

        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your email address.") }
            return
        }
        if (password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your password.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }

            if (state.authMode == AuthMode.SIGN_UP) {
                // STUDENT SIGN UP
                if (state.selectedRole == UserRoleSelection.STUDENT) {
                    val rawIdentity = state.studentIdentityInput.trim()
                    if (rawIdentity.isBlank()) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Please enter or speak your College ID or Aadhaar."
                            )
                        }
                        return@launch
                    }

                    val result = authGateService.signUpStudent(
                        email = email,
                        password = password,
                        identityInput = rawIdentity,
                        isAadhaar = state.studentIdentityType == StudentIdentityType.AADHAAR
                    )

                    when (result) {
                        is AuthResult.Success -> {
                            _uiState.update {
                                it.copy(isLoading = false, successNavigationTarget = "student_home")
                            }
                        }
                        is AuthResult.Error -> {
                            _uiState.update {
                                it.copy(isLoading = false, errorMessage = result.message)
                            }
                        }
                    }
                } else {
                    // Investor sign up delegates to voice register screen
                    _uiState.update {
                        it.copy(isLoading = false, successNavigationTarget = "investor_register")
                    }
                }
            } else {
                // SIGN IN
                when (state.selectedRole) {
                    UserRoleSelection.STUDENT -> {
                        val result = authGateService.signIn(email, password)
                        when (result) {
                            is AuthResult.Success -> {
                                val (userRecord, _) = result.data
                                if (userRecord.role == "student" || userRecord.role.isBlank()) {
                                    _uiState.update {
                                        it.copy(isLoading = false, successNavigationTarget = "student_home")
                                    }
                                } else {
                                    _uiState.update {
                                        it.copy(
                                            isLoading = false,
                                            errorMessage = "This account is registered as ${userRecord.role.uppercase()}. Please select ${userRecord.role.uppercase()} above."
                                        )
                                    }
                                }
                            }
                            is AuthResult.Error -> {
                                _uiState.update {
                                    it.copy(isLoading = false, errorMessage = result.message)
                                }
                            }
                        }
                    }
                    UserRoleSelection.INVESTOR -> {
                        val code = state.investorCodeInput.trim()
                        if (code.isBlank()) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "Please enter your Investor Code to sign in."
                                )
                            }
                            return@launch
                        }

                        val result = authGateService.signInInvestor(email, password, code)
                        when (result) {
                            is AuthResult.Success -> {
                                _uiState.update {
                                    it.copy(isLoading = false, successNavigationTarget = "investor_home")
                                }
                            }
                            is AuthResult.Error -> {
                                _uiState.update {
                                    it.copy(isLoading = false, errorMessage = result.message)
                                }
                            }
                        }
                    }
                    UserRoleSelection.ADMIN -> {
                        val result = authGateService.signIn(email, password)
                        when (result) {
                            is AuthResult.Success -> {
                                val (userRecord, _) = result.data
                                if (userRecord.role == "admin" || email.equals(AppConstants.SEEDED_ADMIN_EMAIL, ignoreCase = true)) {
                                    _uiState.update {
                                        it.copy(isLoading = false, successNavigationTarget = "admin")
                                    }
                                } else {
                                    _uiState.update {
                                        it.copy(
                                            isLoading = false,
                                            errorMessage = "Access denied: Account is not an administrator."
                                        )
                                    }
                                }
                            }
                            is AuthResult.Error -> {
                                _uiState.update {
                                    it.copy(isLoading = false, errorMessage = result.message)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun sendPasswordReset() {
        val email = _uiState.value.emailInput.trim()
        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter your email above and tap Forgot Password.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authGateService.sendPasswordReset(email)
            when (result) {
                is AuthResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            infoMessage = "Password reset email sent to $email. Please check your inbox."
                        )
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }
    }

    fun clearNavigationTarget() {
        _uiState.update { it.copy(successNavigationTarget = null) }
    }

    override fun onCleared() {
        super.onCleared()
        speechHelper?.destroy()
        speechHelper = null
    }
}