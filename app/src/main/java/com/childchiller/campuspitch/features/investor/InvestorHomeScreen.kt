package com.childchiller.campuspitch.features.investor

import android.Manifest
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.QuestionAnswer
import com.childchiller.campuspitch.core.models.ChatMessageRecord
import com.childchiller.campuspitch.core.models.MvpPitchRecord
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.childchiller.campuspitch.core.ai.RankedMvpResult
import com.childchiller.campuspitch.core.models.OutreachRecord
import com.childchiller.campuspitch.core.services.SpeechRecognizerHelper
import com.childchiller.campuspitch.core.theme.CyanAccent
import com.childchiller.campuspitch.core.theme.DarkBackground
import com.childchiller.campuspitch.core.theme.DarkNavySurface
import com.childchiller.campuspitch.core.theme.DeepNavy
import com.childchiller.campuspitch.core.theme.EmeraldLight
import com.childchiller.campuspitch.core.theme.EmeraldPrimary
import com.childchiller.campuspitch.core.theme.GoldStar
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestorHomeScreen(
    onBackToRoleGate: () -> Unit,
    viewModel: InvestorHomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val speechHelper = remember { SpeechRecognizerHelper(context) }
    val isListening by speechHelper.isListening.collectAsState()
    val partialTranscript by speechHelper.partialTranscript.collectAsState()
    val audioLevel by speechHelper.rmsDb.collectAsState()
    val lastResult by speechHelper.lastResult.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) speechHelper.startListening("investor_search")
    }

    DisposableEffect(Unit) {
        onDispose { speechHelper.destroy() }
    }

    LaunchedEffect(isListening, audioLevel) {
        viewModel.setListeningState(isListening, audioLevel)
    }

    LaunchedEffect(partialTranscript) {
        if (partialTranscript.isNotBlank()) {
            viewModel.updateSearchText(partialTranscript)
        }
    }

    LaunchedEffect(lastResult) {
        lastResult?.let { res ->
            val queryText = res.transcriptEnglish.ifBlank { res.transcriptOriginal }
            if (queryText.isNotBlank()) {
                viewModel.updateSearchText(queryText)
                viewModel.executeSearch(
                    utterance = queryText,
                    detectedLang = res.detectedLanguage.bcp47,
                    originalTranscript = res.transcriptOriginal
                )
            }
        }
    }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    fun startListeningChecked() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            if (isListening) {
                speechHelper.stopListening()
            } else {
                speechHelper.startListening("investor_search")
            }
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
                            text = "Investor Deal Flow",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = uiState.investorFundName,
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanAccent
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackToRoleGate) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = CyanAccent
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
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 840.dp)
                    .fillMaxWidth()
            ) {
            // Navigation tabs
            TabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = DarkNavySurface,
                contentColor = CyanAccent,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab]),
                        color = CyanAccent
                    )
                }
            ) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = {
                        Text(
                            text = "Search & Discover",
                            fontWeight = if (uiState.selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (uiState.selectedTab == 0) CyanAccent else LightSlate
                        )
                    }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Pipeline (${uiState.outreachPipeline.size})",
                                fontWeight = if (uiState.selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (uiState.selectedTab == 1) CyanAccent else LightSlate
                            )
                        }
                    }
                )
            }

            if (uiState.selectedTab == 0) {
                // Tab 0: Search & Discover
                SearchAndDiscoverView(
                    uiState = uiState,
                    isListening = isListening,
                    audioLevel = audioLevel,
                    onStartListening = { startListeningChecked() },
                    onSearchTextChange = { viewModel.updateSearchText(it) },
                    onSearchSubmit = { viewModel.executeSearch(it) },
                    onContactClick = { mvp -> viewModel.openConversationForMvp(mvp) },
                    onAcceptClick = { mvp -> viewModel.openAcceptModal(mvp) },
                    onPitchDetailsClick = { mvp -> viewModel.openPitchDetails(mvp) },
                    onRefreshPitches = { viewModel.refreshData() }
                )
            } else {
                // Outreach pipeline dashboard
                PipelineDashboardView(
                    pipeline = uiState.outreachPipeline,
                    onUpdateStatus = { id, status -> viewModel.updateOutreachStatus(id, status) },
                    onOpenConversation = { outreach -> viewModel.openConversation(outreach) }
                )
            }
        }
    }
}

    // Accept Student Pitch & Dispatch Email Dialog
    uiState.acceptModalMvp?.let { mvp ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissAcceptModal() },
            containerColor = DarkNavySurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldLight,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Accept ${mvp.appName} Pitch",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldLight
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Official acceptance will be recorded in the deal pipeline, and an acceptance email will be automatically prepared and dispatched to the student founder:",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = uiState.acceptMessageDraft,
                        onValueChange = { viewModel.updateAcceptDraft(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmAcceptPitch(context) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = DeepNavy
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Confirm & Send Email", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissAcceptModal() }) {
                    Text("Cancel", color = LightSlate)
                }
            }
        )
    }

    // Contact founder dialog
    uiState.contactModalMvp?.let { mvp ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissContactModal() },
            containerColor = DarkNavySurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MailOutline,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Contact ${mvp.appName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "AI Agent drafted introduction message based on deal context:",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = uiState.introMessageDraft,
                        onValueChange = { viewModel.updateIntroDraft(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.sendContactMessage() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanAccent,
                        contentColor = DeepNavy
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send Intro", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissContactModal() }) {
                    Text("Cancel", color = LightSlate)
                }
            }
        )
    }

    // Pitch details dialog
    uiState.selectedPitchForDetails?.let { mvp ->
        PitchDetailsDialog(
            mvp = mvp,
            onDismiss = { viewModel.dismissPitchDetails() },
            onMessageClick = { target ->
                viewModel.dismissPitchDetails()
                viewModel.openConversationForMvp(target)
            },
            onAcceptClick = { target ->
                viewModel.dismissPitchDetails()
                viewModel.openAcceptModal(target)
            }
        )
    }

    // Messaging dialog
    uiState.activeConversation?.let { conversation ->
        InvestorConversationDialog(
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

@Composable
private fun SearchAndDiscoverView(
    uiState: InvestorHomeUiState,
    isListening: Boolean,
    audioLevel: Float,
    onStartListening: () -> Unit,
    onSearchTextChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    onContactClick: (MvpPitchRecord) -> Unit,
    onAcceptClick: (MvpPitchRecord) -> Unit,
    onPitchDetailsClick: (MvpPitchRecord) -> Unit,
    onRefreshPitches: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Voice & Text Spoken Search Input Card (Zero Forms)
        item {
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
                        text = "Spoken Deal Flow Search",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Speak or type thesis, target sectors, or INR investment range",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = if (isListening) listOf(RoseError, DeepNavy) else listOf(CyanAccent, EmeraldPrimary, DeepNavy)
                                )
                            )
                            .clickable { onStartListening() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Search Mic",
                            tint = DeepNavy,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isListening) "Listening... tap to search" else "Tap mic to speak search",
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
                        value = uiState.searchUtterance,
                        onValueChange = onSearchTextChange,
                        placeholder = { Text("Speak or type query (e.g. Hospital apps between 1-5 cr)...") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (uiState.isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = CyanAccent,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                IconButton(onClick = { onSearchSubmit(uiState.searchUtterance) }) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
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
        }

        // Query tags
        uiState.activeQuery?.let { query ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                    border = BorderStroke(1.dp, SurfaceBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Agent Structured Filters",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = true,
                                onClick = {},
                                label = { Text(query.targetSector ?: "All Sectors") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DarkNavySurface,
                                    selectedLabelColor = EmeraldLight
                                )
                            )
                            FilterChip(
                                selected = true,
                                onClick = {},
                                label = { Text(query.formattedRange) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DarkNavySurface,
                                    selectedLabelColor = CyanAccent
                                )
                            )
                        }
                    }
                }
            }
        }

        // Search Results Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ranked Deal Results (${uiState.rankedResults.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Ranked by AI Fit",
                    style = MaterialTheme.typography.bodySmall,
                    color = LightSlate
                )
            }
        }

        if (uiState.rankedResults.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkNavySurface),
                    border = BorderStroke(1.dp, SurfaceBorder),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Student Pitches Matching Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "When students upload their pitches from Student Founder Studio, they will appear here in real-time. Tap below to refresh deal flow.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateMuted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = onRefreshPitches,
                            border = BorderStroke(1.dp, CyanAccent),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Refresh Live Deal Flow", color = CyanAccent)
                        }
                    }
                }
            }
        }

        // Ranked MVP Result Cards
        items(uiState.rankedResults, key = { it.mvp.id }) { result ->
            RankedMvpCard(
                result = result,
                onContactClick = { onContactClick(result.mvp) },
                onAcceptClick = { onAcceptClick(result.mvp) },
                onPitchDetailsClick = { onPitchDetailsClick(result.mvp) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RankedMvpCard(
    result: RankedMvpResult,
    onContactClick: () -> Unit,
    onAcceptClick: () -> Unit,
    onPitchDetailsClick: () -> Unit
) {
    val mvp = result.mvp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPitchDetailsClick() },
        colors = CardDefaults.cardColors(containerColor = DarkNavySurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (result.relevanceScore >= 0.75) EmeraldLight.copy(alpha = 0.6f) else SurfaceBorder
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: App Name + Match Score Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = mvp.appName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = mvp.sector,
                        style = MaterialTheme.typography.bodySmall,
                        color = CyanAccent
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            color = if (result.relevanceScore >= 0.75) EmeraldPrimary.copy(alpha = 0.2f) else SurfaceElevated,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = GoldStar,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${(result.relevanceScore * 100).toInt()}% Match",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (result.relevanceScore >= 0.75) EmeraldLight else LightSlate
                        )
                    }
                }

                FilterChip(
                    selected = false,
                    onClick = onPitchDetailsClick,
                    label = { Text("Details", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = DeepNavy,
                        labelColor = CyanAccent
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Summary card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepNavy, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = result.oneLineAiSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Raise Amount & Stage Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = {
                        Text(
                            text = if (mvp.raiseAmountInr > 0) "Raising ₹${InrParser.formatInr(mvp.raiseAmountInr)}" else "Pre-seed",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldPrimary.copy(alpha = 0.15f),
                        selectedLabelColor = EmeraldLight
                    )
                )
                FilterChip(
                    selected = false,
                    onClick = {},
                    label = { Text("Stage: ${mvp.stage}") },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = SurfaceElevated,
                        labelColor = LightSlate
                    )
                )
            }

            // Match Reasons
            if (result.matchReasons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (reason in result.matchReasons) {
                        Text(
                            text = "• $reason",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Row: Accept Pitch & Message Founder
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onAcceptClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = DeepNavy
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Accept Pitch",
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onContactClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                    border = BorderStroke(1.dp, CyanAccent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MailOutline,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Message",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Investor Deal Pipeline Dashboard.
 * Stages: Contacted > Replied > Meeting > Term sheet > Funded / Declined
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PipelineDashboardView(
    pipeline: List<OutreachRecord>,
    onUpdateStatus: (String, String) -> Unit,
    onOpenConversation: (OutreachRecord) -> Unit
) {
    if (pipeline.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = null,
                    tint = SlateMuted,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Outreach In Pipeline Yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Search for MVPs in the Discover tab and tap 'Contact Student' to initiate conversations and track deals.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Students Contacted (${pipeline.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${pipeline.count { it.status.equals("Meeting", ignoreCase = true) }} Meetings • ${pipeline.count { it.status.equals("Funded", ignoreCase = true) }} Funded",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldLight,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            items(pipeline, key = { it.id }) { record ->
                PipelineRecordCard(
                    record = record,
                    onUpdateStatus = { newStatus -> onUpdateStatus(record.id, newStatus) },
                    onOpenConversation = { onOpenConversation(record) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PipelineRecordCard(
    record: OutreachRecord,
    onUpdateStatus: (String) -> Unit,
    onOpenConversation: () -> Unit
) {
    val stages = listOf("Accepted", "Contacted", "Replied", "Meeting", "Term sheet", "Funded", "Declined")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkNavySurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.mvpAppName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Box(
                    modifier = Modifier
                        .background(
                            when (record.status) {
                                "Accepted", "Funded" -> EmeraldPrimary.copy(alpha = 0.2f)
                                "Term sheet" -> CyanAccent.copy(alpha = 0.2f)
                                "Declined" -> RoseError.copy(alpha = 0.2f)
                                else -> SurfaceElevated
                            },
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = record.status,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (record.status) {
                            "Accepted", "Funded" -> EmeraldLight
                            "Term sheet" -> CyanAccent
                            "Declined" -> RoseError
                            else -> TextSecondary
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "\"${record.introMessage}\"",
                style = MaterialTheme.typography.bodySmall,
                color = LightSlate
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = onOpenConversation,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, CyanAccent),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent)
            ) {
                Icon(
                    imageVector = Icons.Default.QuestionAnswer,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open Chat / View Messages", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Advance Deal Stage:",
                style = MaterialTheme.typography.labelSmall,
                color = SlateMuted
            )

            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (stage in stages) {
                    val isCurrent = record.status == stage
                    FilterChip(
                        selected = isCurrent,
                        onClick = { onUpdateStatus(stage) },
                        label = { Text(stage, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (stage == "Funded") EmeraldPrimary else CyanAccent,
                            selectedLabelColor = DeepNavy,
                            containerColor = SurfaceElevated,
                            labelColor = LightSlate
                        )
                    )
                }
            }
        }
    }
}


/**
 * Detailed Investor Pitch Dialog.
 * Displays full description, sector, formatted INR raise, stage, clickable links,
 * tags, and founder college information with graceful fallbacks.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PitchDetailsDialog(
    mvp: MvpPitchRecord,
    onDismiss: () -> Unit,
    onMessageClick: (MvpPitchRecord) -> Unit,
    onAcceptClick: (MvpPitchRecord) -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkNavySurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = mvp.appName.ifBlank { "Startup Pitch" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    // Category & Stage Tags
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = true,
                            onClick = {},
                            label = { Text(mvp.sector.ifBlank { "General Venture" }) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanAccent.copy(alpha = 0.2f),
                                selectedLabelColor = CyanAccent
                            )
                        )
                        FilterChip(
                            selected = false,
                            onClick = {},
                            label = { Text("Stage: ${mvp.stage}") },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = SurfaceElevated,
                                labelColor = LightSlate
                            )
                        )
                    }
                }

                item {
                    // Funding Request in INR
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DeepNavy, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "FUNDING REQUESTED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LightSlate,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (mvp.raiseAmountInr > 0) "₹${InrParser.formatInr(mvp.raiseAmountInr)}" else "Pre-seed",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldLight
                                )
                            }
                            if (mvp.raiseOriginalWording.isNotBlank()) {
                                Text(
                                    text = "Spoken: \"${mvp.raiseOriginalWording}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateMuted
                                )
                            }
                        }
                    }
                }

                item {
                    // Complete Description
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = LightSlate
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = mvp.description.ifBlank { "No detailed description provided by founder." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }

                // AI Summary if present
                if (!mvp.oneLineAiSummary.isNullOrBlank()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceElevated, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = mvp.oneLineAiSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LightSlate
                                )
                            }
                        }
                    }
                }

                // Founder details
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(
                                text = "Founder",
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateMuted
                            )
                            Text(
                                text = mvp.ownerName.ifBlank { "Campus Founder" },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Institution",
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateMuted
                            )
                            Text(
                                text = mvp.ownerCollege.ifBlank { "Campus Network" },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }
                }

                // Project & Demo Links
                if (mvp.links.isNotEmpty()) {
                    item {
                        Text(
                            text = "Links & Resources",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = LightSlate
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            mvp.links.forEach { rawLink ->
                                val cleanLink = rawLink.trim()
                                if (cleanLink.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                val uri = if (!cleanLink.startsWith("http://") && !cleanLink.startsWith("https://")) {
                                                    Uri.parse("https://$cleanLink")
                                                } else {
                                                    Uri.parse(cleanLink)
                                                }
                                                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(intent)
                                            } catch (_: Exception) {
                                                Toast.makeText(context, "Invalid link: $cleanLink", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Launch,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = CyanAccent
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = cleanLink.take(32) + if (cleanLink.length > 32) "..." else "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CyanAccent
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Uploaded Tags / Files / Assets
                if (mvp.tags.isNotEmpty()) {
                    item {
                        Text(
                            text = "Tags & Uploaded Files",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = LightSlate
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            mvp.tags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .background(SurfaceElevated, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LightSlate
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onAcceptClick(mvp)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = DeepNavy
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Accept Pitch", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    onDismiss()
                    onMessageClick(mvp)
                },
                border = BorderStroke(1.dp, CyanAccent),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent)
            ) {
                Icon(
                    imageVector = Icons.Default.MailOutline,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Message Founder", fontWeight = FontWeight.Bold)
            }
        }
    )
}

/**
 * Two-way in-app messaging interface for institutional investors.
 */
@Composable
private fun InvestorConversationDialog(
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
                        text = "Chat: ${conversation.mvpAppName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Status: ${conversation.status}",
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
                // Error banner
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

                // Messages list
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
                                text = "No messages yet.\nType your message below to begin.",
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
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        reverseLayout = false
                    ) {
                        items(messages, key = { it.id.ifBlank { it.timestamp.toString() } }) { msg ->
                            val isMe = msg.senderRole == "investor"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .widthIn(max = 260.dp)
                                        .background(
                                            color = if (isMe) EmeraldPrimary.copy(alpha = 0.2f) else DeepNavy,
                                            shape = RoundedCornerShape(
                                                topStart = 12.dp,
                                                topEnd = 12.dp,
                                                bottomStart = if (isMe) 12.dp else 2.dp,
                                                bottomEnd = if (isMe) 2.dp else 12.dp
                                            )
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isMe) EmeraldLight.copy(alpha = 0.4f) else SurfaceBorder,
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
                                            text = if (isMe) "You (${msg.senderName})" else msg.senderName.ifBlank { "Founder" },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isMe) EmeraldLight else CyanAccent
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

                // Input bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = draftText,
                        onValueChange = onDraftChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Write a message...", color = SlateMuted) },
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
                            contentDescription = "Send",
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
