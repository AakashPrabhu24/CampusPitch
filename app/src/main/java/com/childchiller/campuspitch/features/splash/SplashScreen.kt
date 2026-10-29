package com.childchiller.campuspitch.features.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.childchiller.campuspitch.core.constants.AppConstants
import com.childchiller.campuspitch.core.navigation.Screen
import com.childchiller.campuspitch.core.services.AuthGateService
import com.childchiller.campuspitch.core.theme.CyanAccent
import com.childchiller.campuspitch.core.theme.DarkBackground
import com.childchiller.campuspitch.core.theme.DeepNavy
import com.childchiller.campuspitch.core.theme.EmeraldLight
import com.childchiller.campuspitch.core.theme.EmeraldPrimary
import com.childchiller.campuspitch.core.theme.LightSlate
import com.childchiller.campuspitch.core.widgets.CopyrightFooter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onRouteResolved: (String) -> Unit
) {
    val scale = remember { Animatable(0.7f) }
    val alpha = remember { Animatable(0f) }
    val authGate = remember { AuthGateService() }

    LaunchedEffect(Unit) {
        val animJob = launch {
            scale.animateTo(1f, animationSpec = tween(900))
            alpha.animateTo(1f, animationSpec = tween(700))
        }

        val sessionResult = authGate.resolveCurrentUserSession()
        animJob.join()
        delay(400) // Brief aesthetic hold for branding

        if (sessionResult.isSuccess) {
            val (_, role, investorStatus) = sessionResult.getOrThrow()
            when (role) {
                "admin" -> onRouteResolved(Screen.Admin.route)
                "investor" -> {
                    if (investorStatus.equals("verified", ignoreCase = true) ||
                        investorStatus.equals("approved", ignoreCase = true)
                    ) {
                        onRouteResolved(Screen.InvestorHome.route)
                    } else {
                        onRouteResolved(Screen.InvestorPending.route)
                    }
                }
                else -> onRouteResolved(Screen.StudentHome.route)
            }
        } else {
            onRouteResolved(Screen.RoleGate.route)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Glowing circular logo icon
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(scale.value)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(CyanAccent, EmeraldPrimary, DeepNavy)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "CampusPitch Voice AI",
                    tint = DeepNavy,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = AppConstants.APP_NAME,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = EmeraldLight,
                modifier = Modifier.alpha(alpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = AppConstants.APP_TAGLINE,
                style = MaterialTheme.typography.bodyMedium,
                color = CyanAccent,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(alpha.value)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = AppConstants.MANDATORY_COPYRIGHT,
                style = MaterialTheme.typography.bodySmall,
                color = LightSlate,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(alpha.value)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            CopyrightFooter()
        }
    }
}