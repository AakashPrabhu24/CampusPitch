package com.childchiller.campuspitch.features.onboarding

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.childchiller.campuspitch.core.constants.AppConstants
import com.childchiller.campuspitch.core.theme.CyanAccent
import com.childchiller.campuspitch.core.theme.DarkBackground
import com.childchiller.campuspitch.core.theme.DarkNavySurface
import com.childchiller.campuspitch.core.theme.DeepNavy
import com.childchiller.campuspitch.core.theme.EmeraldLight
import com.childchiller.campuspitch.core.theme.EmeraldPrimary
import com.childchiller.campuspitch.core.theme.LightSlate
import com.childchiller.campuspitch.core.theme.RoseError
import com.childchiller.campuspitch.core.theme.SlateMuted
import com.childchiller.campuspitch.core.theme.SurfaceBorder
import com.childchiller.campuspitch.core.theme.SurfaceElevated
import com.childchiller.campuspitch.core.theme.TextPrimary
import com.childchiller.campuspitch.core.theme.TextSecondary
import com.childchiller.campuspitch.core.widgets.CopyrightFooter
import com.childchiller.campuspitch.core.widgets.VoiceWaveformBar

@Composable
fun RoleGateScreen(
    onNavigateToStudentHome: () -> Unit,
    onNavigateToInvestorHome: () -> Unit,
    onNavigateToInvestorRegister: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    viewModel: RoleGateViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var permissionDeniedMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.initializeSpeech(context)
            viewModel.toggleVoiceInput()
        } else {
            permissionDeniedMessage = "Microphone permission is needed to speak your credentials. You can also type below."
        }
    }

    LaunchedEffect(Unit) {
        viewModel.initializeSpeech(context)
    }

    LaunchedEffect(uiState.successNavigationTarget) {
        when (uiState.successNavigationTarget) {
            "student_home" -> {
                viewModel.clearNavigationTarget()
                onNavigateToStudentHome()
            }
            "investor_home" -> {
                viewModel.clearNavigationTarget()
                onNavigateToInvestorHome()
            }
            "investor_register" -> {
                viewModel.clearNavigationTarget()
                onNavigateToInvestorRegister()
            }
            "admin" -> {
                viewModel.clearNavigationTarget()
                onNavigateToAdmin()
            }
        }
    }

    fun startListeningWithPermission() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.toggleVoiceInput()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = { CopyrightFooter() }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 580.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // App Title
                Text(
                    text = "Welcome to ${AppConstants.APP_NAME}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = AppConstants.APP_TAGLINE,
                    style = MaterialTheme.typography.titleSmall,
                    color = EmeraldLight,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = AppConstants.MANDATORY_COPYRIGHT,
                    style = MaterialTheme.typography.labelSmall,
                    color = LightSlate,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Role Selection Cards (Student, Investor, Admin)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isStudent = uiState.selectedRole == UserRoleSelection.STUDENT
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.selectRole(UserRoleSelection.STUDENT) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isStudent) DarkNavySurface else SurfaceElevated
                        ),
                        border = BorderStroke(
                            width = if (isStudent) 2.dp else 1.dp,
                            color = if (isStudent) EmeraldPrimary else SurfaceBorder
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = "Student",
                                tint = if (isStudent) EmeraldLight else TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Student",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (isStudent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isStudent) TextPrimary else TextSecondary
                            )
                        }
                    }

                    val isInvestor = uiState.selectedRole == UserRoleSelection.INVESTOR
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.selectRole(UserRoleSelection.INVESTOR) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isInvestor) DarkNavySurface else SurfaceElevated
                        ),
                        border = BorderStroke(
                            width = if (isInvestor) 2.dp else 1.dp,
                            color = if (isInvestor) CyanAccent else SurfaceBorder
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = "Investor",
                                tint = if (isInvestor) CyanAccent else TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Investor",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (isInvestor) FontWeight.Bold else FontWeight.Normal,
                                color = if (isInvestor) TextPrimary else TextSecondary
                            )
                        }
                    }

                    val isAdmin = uiState.selectedRole == UserRoleSelection.ADMIN
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.selectRole(UserRoleSelection.ADMIN) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAdmin) DarkNavySurface else SurfaceElevated
                        ),
                        border = BorderStroke(
                            width = if (isAdmin) 2.dp else 1.dp,
                            color = if (isAdmin) EmeraldPrimary else SurfaceBorder
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Admin",
                                tint = if (isAdmin) EmeraldLight else TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Admin",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (isAdmin) FontWeight.Bold else FontWeight.Normal,
                                color = if (isAdmin) TextPrimary else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sign In vs Sign Up Tab
                if (uiState.selectedRole != UserRoleSelection.ADMIN) {
                    TabRow(
                        selectedTabIndex = if (uiState.authMode == AuthMode.SIGN_IN) 0 else 1,
                        containerColor = DarkNavySurface,
                        contentColor = EmeraldLight,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    ) {
                        Tab(
                            selected = uiState.authMode == AuthMode.SIGN_IN,
                            onClick = { if (uiState.authMode != AuthMode.SIGN_IN) viewModel.toggleAuthMode() },
                            text = { Text("Sign In", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = uiState.authMode == AuthMode.SIGN_UP,
                            onClick = { if (uiState.authMode != AuthMode.SIGN_UP) viewModel.toggleAuthMode() },
                            text = { Text("Create Account", fontWeight = FontWeight.Bold) }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Voice Mic Button for spoken credentials
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = if (uiState.isListening) {
                                    listOf(RoseError, DeepNavy)
                                } else {
                                    listOf(CyanAccent, EmeraldPrimary, DeepNavy)
                                }
                            )
                        )
                        .clickable { startListeningWithPermission() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (uiState.isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = DeepNavy,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (uiState.isListening) "Listening... tap to finish" else "Tap mic to speak your credentials",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (uiState.isListening) RoseError else EmeraldLight
                )

                AnimatedVisibility(visible = uiState.isListening) {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        VoiceWaveformBar(
                            isListening = uiState.isListening,
                            audioLevel = uiState.audioLevel
                        )
                        if (uiState.speechTranscript.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "\"${uiState.speechTranscript}\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = LightSlate,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Email Field
                OutlinedTextField(
                    value = uiState.emailInput,
                    onValueChange = { viewModel.onEmailChange(it) },
                    label = { Text("Email Address") },
                    placeholder = { Text("founder@campus.edu or partner@fund.vc") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = EmeraldPrimary)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password Field
                OutlinedTextField(
                    value = uiState.passwordInput,
                    onValueChange = { viewModel.onPasswordChange(it) },
                    label = { Text("Password") },
                    placeholder = { Text("At least 6 characters") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = EmeraldPrimary)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Role-Specific Fields
                if (uiState.selectedRole == UserRoleSelection.STUDENT && uiState.authMode == AuthMode.SIGN_UP) {
                    // Identity selector (College ID vs Aadhaar)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val isCollegeId = uiState.studentIdentityType == StudentIdentityType.COLLEGE_ID
                        OutlinedButton(
                            onClick = { viewModel.selectStudentIdentityType(StudentIdentityType.COLLEGE_ID) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isCollegeId) EmeraldPrimary.copy(alpha = 0.2f) else DarkNavySurface
                            ),
                            border = BorderStroke(1.dp, if (isCollegeId) EmeraldPrimary else SurfaceBorder),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "College ID (Recommended)",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isCollegeId) EmeraldLight else TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        val isAadhaar = uiState.studentIdentityType == StudentIdentityType.AADHAAR
                        OutlinedButton(
                            onClick = { viewModel.selectStudentIdentityType(StudentIdentityType.AADHAAR) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isAadhaar) EmeraldPrimary.copy(alpha = 0.2f) else DarkNavySurface
                            ),
                            border = BorderStroke(1.dp, if (isAadhaar) EmeraldPrimary else SurfaceBorder),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "Aadhaar",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isAadhaar) EmeraldLight else TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = uiState.studentIdentityInput,
                        onValueChange = { viewModel.onStudentIdentityChange(it) },
                        label = {
                            Text(
                                if (uiState.studentIdentityType == StudentIdentityType.COLLEGE_ID)
                                    "College ID or Roll Number"
                                else
                                    "Aadhaar Number (12 digits)"
                            )
                        },
                        placeholder = {
                            Text(
                                if (uiState.studentIdentityType == StudentIdentityType.COLLEGE_ID)
                                    "e.g. IITB-2026-CS42"
                                else
                                    "e.g. 5432 1098 7654"
                            )
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = EmeraldPrimary)
                        },
                        visualTransformation = if (uiState.studentIdentityType == StudentIdentityType.AADHAAR)
                            PasswordVisualTransformation()
                        else
                            VisualTransformation.None,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (uiState.studentIdentityType == StudentIdentityType.AADHAAR)
                            "Privacy guarantee: Aadhaar is hashed with a cryptographic salt. Full numbers are never stored."
                        else
                            "Name, college, and mobile will be recorded naturally by voice on your first run.",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateMuted
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (uiState.selectedRole == UserRoleSelection.INVESTOR && uiState.authMode == AuthMode.SIGN_IN) {
                    OutlinedTextField(
                        value = uiState.investorCodeInput,
                        onValueChange = { viewModel.onInvestorCodeChange(it) },
                        label = { Text("Investor Verification Code") },
                        placeholder = { Text("e.g. CCV-DEMO-2026") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = CyanAccent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Reviewer demo shortcut button
                    OutlinedButton(
                        onClick = { viewModel.fillDemoInvestorCredentials() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f))
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Fill Demo Investor: ${AppConstants.SEEDED_DEMO_INVESTOR_CODE}", style = MaterialTheme.typography.labelSmall, color = CyanAccent)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (uiState.selectedRole == UserRoleSelection.ADMIN) {
                    OutlinedButton(
                        onClick = { viewModel.fillAdminCredentials() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f))
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Fill Seeded Admin: ${AppConstants.SEEDED_ADMIN_EMAIL}", style = MaterialTheme.typography.labelSmall, color = EmeraldLight)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Submit Button
                Button(
                    onClick = { viewModel.submitAuth() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !uiState.isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.selectedRole == UserRoleSelection.INVESTOR) CyanAccent else EmeraldPrimary,
                        contentColor = DarkBackground
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = DarkBackground, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        val label = when {
                            uiState.selectedRole == UserRoleSelection.ADMIN -> "Sign In as Administrator"
                            uiState.selectedRole == UserRoleSelection.INVESTOR && uiState.authMode == AuthMode.SIGN_UP -> "Register Institutional Fund"
                            uiState.selectedRole == UserRoleSelection.INVESTOR -> "Sign In to Investor Studio"
                            uiState.authMode == AuthMode.SIGN_UP -> "Create Student Account"
                            else -> "Sign In as Student Founder"
                        }
                        Text(text = label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Forgot Password Button
                if (uiState.authMode == AuthMode.SIGN_IN) {
                    TextButton(onClick = { viewModel.sendPasswordReset() }) {
                        Text("Forgot password? Send reset email", color = TextSecondary, fontSize = 12.sp)
                    }
                }

                // Messages
                if (uiState.infoMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, EmeraldPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text(
                            text = uiState.infoMessage ?: "",
                            color = EmeraldLight,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (uiState.errorMessage != null || permissionDeniedMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = RoseError.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, RoseError),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text(
                            text = uiState.errorMessage ?: permissionDeniedMessage ?: "",
                            color = RoseError,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}