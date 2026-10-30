package com.childchiller.campuspitch.features.investor

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.childchiller.campuspitch.core.services.SpeechRecognizerHelper
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestorRegisterScreen(
    onBack: () -> Unit,
    onApprovedCodeIssued: (String) -> Unit,
    viewModel: InvestorRegisterViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var passwordInput by remember { mutableStateOf("InvestorPassword2026!") }
    var showPassword by remember { mutableStateOf(false) }

    val speechHelper = remember { SpeechRecognizerHelper(context) }
    val isListening by speechHelper.isListening.collectAsState()
    val partialTranscript by speechHelper.partialTranscript.collectAsState()
    val audioLevel by speechHelper.rmsDb.collectAsState()
    val lastResult by speechHelper.lastResult.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) speechHelper.startListening("investor_register")
    }

    DisposableEffect(Unit) {
        onDispose { speechHelper.destroy() }
    }

    LaunchedEffect(isListening, audioLevel) {
        viewModel.setListeningState(isListening, audioLevel)
    }

    LaunchedEffect(partialTranscript) {
        if (partialTranscript.isNotBlank()) {
            viewModel.updateInputSentence(partialTranscript)
        }
    }

    LaunchedEffect(lastResult) {
        lastResult?.let { res ->
            val text = res.transcriptEnglish.ifBlank { res.transcriptOriginal }
            if (text.isNotBlank()) {
                viewModel.updateInputSentence(text)
                viewModel.processRegistrationUtterance(text)
            }
        }
    }

    LaunchedEffect(uiState.isSubmittedPending, uiState.issuedCode) {
        if (uiState.isSubmittedPending && uiState.issuedCode == null) {
            onApprovedCodeIssued("")
        }
    }

    fun startListeningChecked() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            if (isListening) speechHelper.stopListening() else speechHelper.startListening("investor_register")
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Investor Registration Agent",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = CyanAccent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkNavySurface)
            )
        },
        bottomBar = { CopyrightFooter() }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 700.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Natural Language Voice & Text Intake Box (Zero Fields)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkNavySurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Speak Your Fund Credentials",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "State fund name, AIF category, corporate email, phone, and website",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = if (isListening) listOf(RoseError, DeepNavy) else listOf(CyanAccent, EmeraldPrimary, DeepNavy)
                                )
                            )
                            .clickable { startListeningChecked() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = DeepNavy,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isListening) "Listening... tap mic to analyze" else "Tap mic to speak credentials",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isListening) RoseError else CyanAccent
                    )

                    AnimatedVisibility(visible = isListening) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            VoiceWaveformBar(isListening = isListening, audioLevel = audioLevel)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = uiState.inputSentence,
                        onValueChange = { viewModel.updateInputSentence(it) },
                        placeholder = { Text("Or speak/type full sentence here...") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (uiState.isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = CyanAccent,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                IconButton(
                                    onClick = {
                                        viewModel.processRegistrationUtterance(uiState.inputSentence)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Extract",
                                        tint = CyanAccent
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Error display
            uiState.errorMessage?.let { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkNavySurface),
                    border = BorderStroke(1.dp, RoseError),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = RoseError,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = msg, color = RoseError, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Follow-up question card
            uiState.result?.followUpQuestion?.let { question ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                    border = BorderStroke(1.dp, CyanAccent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.QuestionAnswer,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Agent Clarification Needed:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = question,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // Extracted & Validated "Did I get this right?" Card
            uiState.result?.profile?.let { profile ->
                val validation = uiState.result!!.validation

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                    border = BorderStroke(2.dp, if (validation.isFullyValid) EmeraldLight else CyanAccent),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Did I get this right?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent
                            )
                            Icon(
                                imageVector = if (validation.isFullyValid) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (validation.isFullyValid) EmeraldLight else RoseError,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = profile.fundName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = true,
                                onClick = {},
                                label = { Text(profile.aifCategory) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DarkNavySurface,
                                    selectedLabelColor = CyanAccent
                                )
                            )
                            FilterChip(
                                selected = validation.doesEmailDomainMatchWebsite,
                                onClick = {},
                                label = {
                                    Text(if (validation.doesEmailDomainMatchWebsite) "Domain Match: Verified" else "Domain Mismatch")
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (validation.doesEmailDomainMatchWebsite) DarkNavySurface else RoseError.copy(alpha = 0.2f),
                                    selectedLabelColor = if (validation.doesEmailDomainMatchWebsite) EmeraldLight else RoseError
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(text = "Official Email: ${profile.officialEmail}", color = LightSlate, fontSize = 13.sp)
                        Text(text = "Mobile: +91 ${profile.mobile}", color = LightSlate, fontSize = 13.sp)
                        Text(text = "Website: https://${profile.website}", color = LightSlate, fontSize = 13.sp)

                        // Validation checklist items
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkNavySurface, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            ValidationRow(
                                title = "Valid corporate email address",
                                isValid = validation.isEmailValid
                            )
                            ValidationRow(
                                title = "10-digit Indian mobile (+91)",
                                isValid = validation.isMobileValid
                            )
                            ValidationRow(
                                title = "Email domain matches website domain",
                                isValid = validation.doesEmailDomainMatchWebsite
                            )
                        }

                        if (!uiState.isSubmittedPending && uiState.issuedCode == null) {
                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Account Password") },
                                singleLine = true,
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (showPassword) "Hide password" else "Show password",
                                            tint = CyanAccent
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanAccent,
                                    unfocusedBorderColor = SurfaceBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { viewModel.confirmAndSubmit(passwordInput.ifBlank { "InvestorPassword2026!" }) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = validation.isFullyValid,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanAccent,
                                    contentColor = DeepNavy
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "Register & Issue Investor Code",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Pending review status card
            if (uiState.isSubmittedPending && uiState.issuedCode == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkNavySurface),
                    border = BorderStroke(1.dp, CyanAccent),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Registration Status: Pending Approval",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Your registration has been logged to the Child Chiller Data Lake. An administrator will review credentials before issuing an Investor Code.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateMuted,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { onApprovedCodeIssued("") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanAccent,
                                contentColor = DeepNavy
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "View Application Status",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Approved & Code Issued Card (Acceptance Test T3 Completion)
            uiState.issuedCode?.let { code ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                    border = BorderStroke(2.dp, EmeraldLight),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldLight,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Investor Code Issued!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldLight
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = code,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Your unique Investor Code has been generated! Use this identity key on the Opening Screen to access the Investor Deal Flow, or tap 'Log In Now' below to enter immediately.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LightSlate,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Investor Code", code)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Code copied: $code", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, CyanAccent)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Code", color = CyanAccent)
                            }

                            Button(
                                onClick = { onApprovedCodeIssued(code) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = DeepNavy
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Log In Now", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun ValidationRow(title: String, isValid: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = if (isValid) TextPrimary else RoseError
        )
        Icon(
            imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = null,
            tint = if (isValid) EmeraldLight else RoseError,
            modifier = Modifier.size(16.dp)
        )
    }
}
