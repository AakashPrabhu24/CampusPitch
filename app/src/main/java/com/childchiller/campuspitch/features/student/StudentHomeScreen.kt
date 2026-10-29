package com.childchiller.campuspitch.features.student

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.School
import com.childchiller.campuspitch.core.models.StudentProfileRecord
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.childchiller.campuspitch.core.ai.DetectedLanguageInfo
import com.childchiller.campuspitch.core.constants.AppConstants
import com.childchiller.campuspitch.core.models.ChatMessageRecord
import com.childchiller.campuspitch.core.models.MvpPitchRecord
import com.childchiller.campuspitch.core.models.OutreachRecord
import com.childchiller.campuspitch.core.repository.SyncState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.childchiller.campuspitch.core.utils.InrParser
import com.childchiller.campuspitch.core.widgets.CopyrightFooter
import com.childchiller.campuspitch.core.widgets.VoiceWaveformBar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(
    onBackToRoleGate: () -> Unit,
    viewModel: StudentIntakeViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var editingMvp by remember { mutableStateOf<MvpPitchRecord?>(null) }
    var editingRaiseText by remember { mutableStateOf("") }
    var showLanguageOverrideDialog by remember { mutableStateOf(false) }
    var showGitHubImportDialog by remember { mutableStateOf(false) }
    var gitHubRepoUrlInput by remember { mutableStateOf("") }
    var gitHubInputError by remember { mutableStateOf<String?>(null) }
    var isEditingTranscript by remember { mutableStateOf(false) }
    var editableTranscript by remember { mutableStateOf("") }

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val speechEngine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
            }
        }
        tts = speechEngine
        onDispose {
            speechEngine.stop()
            speechEngine.shutdown()
        }
    }

    LaunchedEffect(uiState.followUpQuestion, ttsReady) {
        val question = uiState.followUpQuestion
        val engine = tts
        if (question != null && engine != null && ttsReady) {
            val bcp47 = uiState.detectedLanguage.bcp47
            val locale = Locale.forLanguageTag(bcp47)
            val availability = engine.isLanguageAvailable(locale)
            if (availability >= TextToSpeech.LANG_AVAILABLE) {
                engine.language = locale
                engine.speak(question, TextToSpeech.QUEUE_FLUSH, null, "follow_up_${System.currentTimeMillis()}")
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = uri.lastPathSegment ?: "pitch_attachment.zip"
            viewModel.attachFileName(name)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.initializeSpeech(context)
            viewModel.toggleVoiceInput()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.initializeSpeech(context)
    }

    fun startListeningChecked() {
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
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Student Founder Studio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Zero fields • Natural voice upload",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldLight
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackToRoleGate) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "Role Gate",
                            tint = EmeraldPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        com.childchiller.campuspitch.core.services.AuthGateService().signOut()
                        onBackToRoleGate()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = TextSecondary
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Student profile dashboard header
            item {
                val profile = uiState.studentProfile ?: StudentProfileRecord(
                    studentName = "Student Founder",
                    collegeName = "IIT Bombay • Computer Science",
                    collegeId = "IITB-2026-CS42",
                    mobile = "9876543210"
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkNavySurface),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldLight,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = profile.studentName ?: "Student Founder",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(EmeraldPrimary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Verified",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldLight,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Text(
                                text = profile.collegeId ?: profile.aadhaarLast4?.let { "Aadhaar: •••• $it" } ?: "ID: IITB-2026-CS42",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = profile.collegeName ?: "IIT Bombay • Computer Science",
                                style = MaterialTheme.typography.bodySmall,
                                color = LightSlate
                            )
                            Text(
                                text = "VC Visibility: Active",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldLight
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // Live metrics bar
                        val targetTotalInr = uiState.uploadedMvps.sumOf { it.raiseAmountInr }
                        val inquiriesCount = uiState.incomingOutreach.size
                        val activeAppsCount = uiState.uploadedMvps.size
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceElevated, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$activeAppsCount App${if (activeAppsCount == 1) "" else "s"} • ₹${InrParser.formatInr(targetTotalInr)} Target",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldLight
                            )
                            Text(
                                text = "$inquiriesCount Investor Inquir${if (inquiriesCount == 1) "y" else "ies"}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = CyanAccent
                            )
                        }
                    }
                }
            }

            // Big Voice Mic & Conversational Input Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkNavySurface),
                    border = BorderStroke(1.dp, SurfaceBorder),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (uiState.conversationState == IntakeConversationState.AWAITING_FOLLOW_UP)
                                "Answer Agent's Follow-up"
                            else
                                "Speak or Type Your Pitch",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Name your app, what it does, and how much you want to raise",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateMuted,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Mic Button
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = if (uiState.isListening)
                                            listOf(RoseError, DeepNavy)
                                        else
                                            listOf(CyanAccent, EmeraldPrimary, DeepNavy)
                                    )
                                )
                                .clickable { startListeningChecked() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (uiState.isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = DeepNavy,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (uiState.isListening) "Listening... tap to process" else "Tap mic to speak",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (uiState.isListening) RoseError else EmeraldLight
                        )

                        AnimatedVisibility(visible = uiState.isListening) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                VoiceWaveformBar(
                                    isListening = uiState.isListening,
                                    audioLevel = uiState.audioLevel
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Natural Language Input Box
                        OutlinedTextField(
                            value = uiState.currentSentence,
                            onValueChange = { viewModel.onSentenceChange(it) },
                            placeholder = {
                                Text(
                                    if (uiState.conversationState == IntakeConversationState.AWAITING_FOLLOW_UP)
                                        "Speak or type your answer..."
                                    else
                                        "Speak or type: \"I want to upload an app called...\""
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(
                                    onClick = { viewModel.submitNaturalSentence(uiState.currentSentence) },
                                    enabled = uiState.currentSentence.isNotBlank() && uiState.conversationState != IntakeConversationState.PROCESSING
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Submit Sentence",
                                        tint = EmeraldPrimary
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = SurfaceBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        AnimatedVisibility(visible = uiState.errorMessage != null) {
                            uiState.errorMessage?.let { err ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp),
                                    colors = CardDefaults.cardColors(containerColor = RoseError.copy(alpha = 0.15f)),
                                    border = BorderStroke(1.dp, RoseError),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = null,
                                            tint = RoseError,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = err,
                                            color = RoseError,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }

                        if (uiState.conversationState == IntakeConversationState.PROCESSING) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    color = EmeraldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Processing speech and extracting pitch details...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldLight
                                )
                            }
                        }
                    }
                }
            }

            // Follow-up question card
            if (uiState.conversationState == IntakeConversationState.AWAITING_FOLLOW_UP && uiState.followUpQuestion != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                        border = BorderStroke(2.dp, CyanAccent),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.QuestionAnswer,
                                        contentDescription = null,
                                        tint = CyanAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Agent Follow-Up",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = CyanAccent
                                    )
                                }
                                Text(
                                    text = "Zero-form follow-up",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SlateMuted
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = uiState.followUpQuestion!!,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Speak or type your answer above. The agent will automatically update your pitch.",
                                style = MaterialTheme.typography.labelSmall,
                                color = LightSlate
                            )
                        }
                    }
                }
            }

            // Pitch confirmation card
            if (uiState.activeDraftMvp != null && uiState.activeDraftMvp!!.appName.isNotBlank() && uiState.conversationState != IntakeConversationState.CONFIRMED_SUCCESS) {
                val draft = uiState.activeDraftMvp!!
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                        border = BorderStroke(2.dp, EmeraldPrimary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = EmeraldLight,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Did I get this right?",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldLight
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.cancelDraft() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel",
                                        tint = SlateMuted
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // "Detected: <language>" Chip (Tap to override)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = true,
                                    onClick = { showLanguageOverrideDialog = true },
                                    label = {
                                        Text(
                                            text = "Detected: ${uiState.detectedLanguage.name} (${uiState.detectedLanguage.bcp47}) ▾",
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyanAccent.copy(alpha = 0.15f),
                                        selectedLabelColor = CyanAccent
                                    ),
                                    border = BorderStroke(1.dp, CyanAccent)
                                )
                                Text(
                                    text = "Tap chip to override",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SlateMuted
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Transcripts Card: Original script + English translation underneath
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DarkNavySurface),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, SurfaceBorder)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Transcript (${uiState.detectedLanguage.name}):",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SlateMuted
                                        )
                                        IconButton(
                                            onClick = {
                                                editableTranscript = uiState.transcriptOriginal.ifBlank { draft.description }
                                                isEditingTranscript = !isEditingTranscript
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Transcript",
                                                tint = CyanAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    if (isEditingTranscript) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        OutlinedTextField(
                                            value = editableTranscript,
                                            onValueChange = { editableTranscript = it },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = CyanAccent,
                                                unfocusedBorderColor = SurfaceBorder,
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(
                                                onClick = { isEditingTranscript = false },
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text("Cancel", fontSize = 11.sp, color = TextSecondary)
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Button(
                                                onClick = {
                                                    viewModel.submitNaturalSentence(editableTranscript)
                                                    isEditingTranscript = false
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = CyanAccent,
                                                    contentColor = DeepNavy
                                                ),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text("Re-parse", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = uiState.transcriptOriginal.ifBlank { draft.description },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary
                                        )
                                        if (uiState.transcriptEnglish.isNotBlank() && uiState.transcriptEnglish != uiState.transcriptOriginal) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "English translation:",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SlateMuted
                                            )
                                            Text(
                                                text = uiState.transcriptEnglish,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = LightSlate
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = draft.appName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = draft.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = LightSlate
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = true,
                                    onClick = {},
                                    label = { Text(draft.sector) }
                                )
                                FilterChip(
                                    selected = true,
                                    onClick = {},
                                    label = { Text("Stage: ${draft.stage}") }
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Formatted INR Card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkNavySurface),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Seeking Investment:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "₹${InrParser.formatInr(draft.raiseAmountInr)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldLight
                                    )
                                }
                            }

                            if (uiState.attachedFileName != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Attached: ${uiState.attachedFileName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanAccent
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Confirmation & Voice Edit Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.confirmAndUploadMvp() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldPrimary,
                                        contentColor = DeepNavy
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Confirm & Upload")
                                }

                                OutlinedButton(
                                    onClick = {
                                        startListeningChecked()
                                    },
                                    modifier = Modifier.weight(1f),
                                    border = BorderStroke(1.dp, CyanAccent),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = CyanAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Voice Edit", color = CyanAccent)
                                }
                            }
                        }
                    }
                }
            }

            // File upload attachment
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkNavySurface),
                    border = BorderStroke(1.dp, SurfaceBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Optional Attachments (Phone / Drive / GitHub)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Storage Access Framework: Pick user-selected APK, ZIP or README without granting all-files access.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateMuted
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    filePickerLauncher.launch(arrayOf("*/*"))
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, EmeraldLight)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = null,
                                    tint = EmeraldLight,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(uiState.attachedFileName ?: "Pick File from Device", color = EmeraldLight)
                            }

                            OutlinedButton(
                                onClick = {
                                    gitHubRepoUrlInput = ""
                                    gitHubInputError = null
                                    showGitHubImportDialog = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, CyanAccent)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Import GitHub", color = CyanAccent)
                            }
                        }
                    }
                }
            }

            // Uploaded MVPs List (Multi-App support)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Published MVPs (${uiState.uploadedMvps.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Central Data Lake live",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldLight
                    )
                }
            }

            items(uiState.uploadedMvps, key = { it.id }) { app ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkNavySurface),
                    border = BorderStroke(1.dp, SurfaceBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "₹${InrParser.formatInr(app.raiseAmountInr)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldLight
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = app.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        app.oneLineAiSummary?.let { summary ->
                            Text(
                                text = "AI Summary: $summary",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanAccent
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = false,
                                    onClick = {},
                                    label = { Text(app.sector, fontSize = 11.sp) }
                                )
                                Text(
                                    text = "Seeking: ${app.raiseOriginalWording}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SlateMuted
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        editingMvp = app
                                        editingRaiseText = app.raiseOriginalWording
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Raise",
                                        tint = CyanAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteMvp(app.id) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete MVP",
                                        tint = RoseError,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Incoming Investor Inquiries (Live Two-Way Sync)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Incoming Investor Inquiries (${uiState.incomingOutreach.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Real-time sync",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldLight
                    )
                }
            }

            if (uiState.incomingOutreach.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkNavySurface.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, SurfaceBorder)
                    ) {
                        Text(
                            text = "No inquiries yet. When verified investors contact your pitch, requests appear here live.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateMuted,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(uiState.incomingOutreach, key = { it.id }) { inquiry ->
                    val isAccepted = inquiry.status.equals("Accepted", ignoreCase = true)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkNavySurface),
                        border = BorderStroke(if (isAccepted) 2.dp else 1.dp, if (isAccepted) EmeraldLight else if (inquiry.status == "Contacted") CyanAccent else SurfaceBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            if (isAccepted) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.15f)),
                                    border = BorderStroke(1.dp, EmeraldLight),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = EmeraldLight,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "🎉 Your app ${inquiry.mvpAppName} has been ACCEPTED by ${inquiry.investorFundName}!",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldLight
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = inquiry.investorFundName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAccepted) EmeraldLight else CyanAccent
                                )
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = when (inquiry.status) {
                                            "Accepted", "Funded" -> EmeraldPrimary.copy(alpha = 0.2f)
                                            "Meeting" -> CyanAccent.copy(alpha = 0.2f)
                                            else -> DeepNavy
                                        }
                                    ),
                                    border = BorderStroke(1.dp, if (isAccepted) EmeraldLight else SurfaceBorder)
                                ) {
                                    Text(
                                        text = inquiry.status.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when (inquiry.status) {
                                            "Accepted", "Funded" -> EmeraldLight
                                            "Meeting" -> CyanAccent
                                            else -> TextPrimary
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Regarding: ${inquiry.mvpAppName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldLight,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = inquiry.introMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.openConversation(inquiry) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                                    border = BorderStroke(1.dp, CyanAccent),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QuestionAnswer,
                                        contentDescription = null,
                                        tint = CyanAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open Chat / Reply", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                if (inquiry.status == "Accepted") {
                                    Button(
                                        onClick = {
                                            viewModel.updateOutreachStatus(inquiry.id, "Meeting")
                                            Toast.makeText(context, "Acknowledged! Meeting requested with ${inquiry.investorFundName}.", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = DeepNavy),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Schedule Call", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else if (inquiry.status == "Contacted") {
                                    Button(
                                        onClick = { viewModel.openConversation(inquiry) },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = DeepNavy),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Reply Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else if (inquiry.status == "Replied") {
                                    Button(
                                        onClick = { viewModel.updateOutreachStatus(inquiry.id, "Meeting") },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = DeepNavy),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Confirm Meeting", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                CopyrightFooter()
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (editingMvp != null) {
        val mvp = editingMvp!!
        AlertDialog(
            onDismissRequest = { editingMvp = null },
            title = { Text("Edit Raise for ${mvp.appName}", color = TextPrimary) },
            text = {
                Column {
                    Text("Enter new target raise (e.g. 2 cr, 50 lakh, 1.5 crore):", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editingRaiseText,
                        onValueChange = { editingRaiseText = it },
                        placeholder = { Text("e.g. 2 cr") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateMvpRaise(mvp.id, editingRaiseText)
                        editingMvp = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Save Live", color = DarkBackground)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { editingMvp = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkNavySurface
        )
    }

    if (showGitHubImportDialog) {
        AlertDialog(
            onDismissRequest = { showGitHubImportDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Import GitHub Repository",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter a public GitHub repository to automatically import your project name, tech stack, and generate your MVP pitch.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = gitHubRepoUrlInput,
                        onValueChange = {
                            gitHubRepoUrlInput = it
                            gitHubInputError = null
                        },
                        placeholder = { Text("e.g. childchiller/medicare or https://github.com/...", color = SlateMuted, fontSize = 12.sp) },
                        singleLine = true,
                        isError = gitHubInputError != null,
                        supportingText = {
                            if (gitHubInputError != null) {
                                Text(gitHubInputError!!, color = RoseError, fontSize = 11.sp)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Quick Demo Repositories (Tap to fill):",
                        color = LightSlate,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                gitHubRepoUrlInput = "childchiller-ventures/campus-pitch"
                                gitHubInputError = null
                            },
                            border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("campus-pitch", fontSize = 10.sp, color = CyanAccent)
                        }
                        OutlinedButton(
                            onClick = {
                                gitHubRepoUrlInput = "childchiller/medicare"
                                gitHubInputError = null
                            },
                            border = BorderStroke(1.dp, EmeraldLight.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("medicare", fontSize = 10.sp, color = EmeraldLight)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val input = gitHubRepoUrlInput.trim()
                        val parsed = com.childchiller.campuspitch.core.services.GitHubRepoService.parseGitHubUrl(input)
                        if (parsed == null) {
                            gitHubInputError = "Please enter valid owner/repo or github.com/owner/repo"
                        } else {
                            showGitHubImportDialog = false
                            viewModel.submitNaturalSentence(input)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanAccent,
                        contentColor = DeepNavy
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Import Now", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showGitHubImportDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkNavySurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showLanguageOverrideDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageOverrideDialog = false },
            title = {
                Text(
                    text = "Override Detected Language",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(AppConstants.SUPPORTED_LANGUAGES) { lang ->
                        val isSelected = uiState.detectedLanguage.bcp47 == lang.bcp47
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.overrideLanguage(
                                        DetectedLanguageInfo(
                                            bcp47 = lang.bcp47,
                                            name = lang.name,
                                            confidence = 1.0
                                        )
                                    )
                                    showLanguageOverrideDialog = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    EmeraldPrimary.copy(alpha = 0.2f)
                                else
                                    SurfaceElevated
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = lang.name,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = lang.nativeName,
                                        color = LightSlate,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Text(
                                    text = lang.bcp47,
                                    color = if (isSelected) EmeraldLight else CyanAccent,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                OutlinedButton(
                    onClick = { showLanguageOverrideDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Close", color = TextSecondary)
                }
            },
            containerColor = DarkNavySurface
        )
    }

    // Messaging dialog
    uiState.activeConversation?.let { conversation ->
        StudentConversationDialog(
            conversation = conversation,
            messages = uiState.conversationMessages,
            isLoading = uiState.isConversationLoading,
            errorMessage = uiState.conversationError,
            draftText = uiState.chatMessageDraft,
            onDraftChange = { viewModel.updateChatDraft(it) },
            onSendMessage = { viewModel.sendConversationMessage() },
            onDismiss = { viewModel.closeConversation() }
        )
    }
}
}

/**
 * Two-way in-app messaging interface for student founders.
 */
@Composable
private fun StudentConversationDialog(
    conversation: OutreachRecord,
    messages: List<ChatMessageRecord>,
    isLoading: Boolean,
    errorMessage: String?,
    draftText: String,
    onDraftChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkNavySurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Chat with ${conversation.investorFundName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Regarding: ${conversation.mvpAppName} (${conversation.status})",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyanAccent
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = LightSlate
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            ) {
                if (!errorMessage.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RoseError.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text(text = errorMessage, color = RoseError, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = CyanAccent, modifier = Modifier.size(32.dp))
                    }
                } else if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.QuestionAnswer,
                                contentDescription = null,
                                tint = LightSlate,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Initial inquiry:\n\"${conversation.introMessage}\"\n\nType your reply below to continue the conversation.",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateMuted
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(messages, key = { it.id.ifBlank { it.timestamp.toString() } }) { msg ->
                            val isMe = msg.senderRole == "student"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .widthIn(max = 260.dp)
                                        .background(
                                            color = if (isMe) CyanAccent.copy(alpha = 0.2f) else DeepNavy,
                                            shape = RoundedCornerShape(
                                                topStart = 12.dp,
                                                topEnd = 12.dp,
                                                bottomStart = if (isMe) 12.dp else 2.dp,
                                                bottomEnd = if (isMe) 2.dp else 12.dp
                                            )
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isMe) CyanAccent.copy(alpha = 0.4f) else EmeraldLight.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(
                                                topStart = 12.dp,
                                                topEnd = 12.dp,
                                                bottomStart = if (isMe) 12.dp else 2.dp,
                                                bottomEnd = if (isMe) 2.dp else 12.dp
                                            )
                                        )
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = if (isMe) "You (Founder)" else msg.senderName.ifBlank { "Investor" },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isMe) CyanAccent else EmeraldLight
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = msg.text,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = draftText,
                        onValueChange = onDraftChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Write your reply...", color = SlateMuted) },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onSendMessage,
                        enabled = draftText.isNotBlank(),
                        modifier = Modifier
                            .background(if (draftText.isNotBlank()) CyanAccent else SurfaceElevated, CircleShape)
                            .size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Reply",
                            tint = if (draftText.isNotBlank()) DeepNavy else LightSlate,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}
