package com.childchiller.campuspitch.features.investor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.childchiller.campuspitch.core.repository.FirestoreProfileRepository
import com.childchiller.campuspitch.core.services.AuthGateService
import com.childchiller.campuspitch.core.theme.CyanAccent
import com.childchiller.campuspitch.core.theme.DarkBackground
import com.childchiller.campuspitch.core.theme.DarkNavySurface
import com.childchiller.campuspitch.core.theme.EmeraldLight
import com.childchiller.campuspitch.core.theme.EmeraldPrimary
import com.childchiller.campuspitch.core.theme.SlateMuted
import com.childchiller.campuspitch.core.theme.SurfaceBorder
import com.childchiller.campuspitch.core.theme.TextPrimary
import com.childchiller.campuspitch.core.theme.TextSecondary
import com.childchiller.campuspitch.core.widgets.CopyrightFooter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestorPendingScreen(
    onVerified: () -> Unit,
    onSignOut: () -> Unit
) {
    val authGate = remember { AuthGateService() }
    val profileRepo = remember { FirestoreProfileRepository() }
    val scope = rememberCoroutineScope()
    var isChecking by remember { mutableStateOf(false) }
    var statusNotice by remember { mutableStateOf<String?>(null) }

    fun checkStatus() {
        val uid = authGate.currentUid ?: return
        isChecking = true
        statusNotice = null
        scope.launch {
            try {
                val profile = profileRepo.observeInvestorProfile(uid).firstOrNull()
                isChecking = false
                if (profile != null && profile.isVerified) {
                    onVerified()
                } else {
                    statusNotice = "Status is still pending verification. Please contact the administrator or check back soon."
                }
            } catch (e: Exception) {
                isChecking = false
                statusNotice = "Unable to check verification status right now."
            }
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Investor Access Pending",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                actions = {
                    IconButton(onClick = {
                        authGate.signOut()
                        onSignOut()
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
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Card(
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = CyanAccent.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, CyanAccent)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Application Under Review",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your institutional investor registration has been submitted and is currently pending verification by Child Chiller Ventures compliance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkNavySurface),
                    border = BorderStroke(1.dp, SurfaceBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Access Policy Notice",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldLight
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "To protect student intellectual property, student pitch decks and deal metrics are restricted to verified institutional investors with an active Investor Code.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateMuted
                        )
                    }
                }

                if (statusNotice != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = statusNotice ?: "",
                        color = CyanAccent,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            authGate.signOut()
                            onSignOut()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Sign Out", color = TextSecondary)
                    }

                    Button(
                        onClick = { checkStatus() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isChecking
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = DarkBackground, strokeWidth = 2.dp)
                        } else {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Check Status", color = DarkBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}