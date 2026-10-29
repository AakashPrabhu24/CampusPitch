package com.childchiller.campuspitch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.childchiller.campuspitch.core.constants.AppConstants
import com.childchiller.campuspitch.core.navigation.Screen
import com.childchiller.campuspitch.core.theme.CampusPitchTheme
import com.childchiller.campuspitch.core.theme.DarkBackground
import com.childchiller.campuspitch.core.theme.DeepNavy
import com.childchiller.campuspitch.core.theme.EmeraldLight
import com.childchiller.campuspitch.core.theme.EmeraldPrimary
import com.childchiller.campuspitch.core.theme.TextPrimary
import com.childchiller.campuspitch.core.theme.TextSecondary
import com.childchiller.campuspitch.core.widgets.CopyrightFooter
import com.childchiller.campuspitch.features.onboarding.RoleGateScreen
import com.childchiller.campuspitch.features.splash.SplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CampusPitchTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CampusPitchNavGraph()
                }
            }
        }
    }
}

@Composable
fun CampusPitchNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onRouteResolved = { targetRoute ->
                    navController.navigate(targetRoute) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.RoleGate.route) {
            RoleGateScreen(
                onNavigateToStudentHome = {
                    navController.navigate(Screen.StudentHome.route)
                },
                onNavigateToInvestorHome = {
                    navController.navigate(Screen.InvestorHome.route)
                },
                onNavigateToInvestorRegister = {
                    navController.navigate(Screen.InvestorRegister.route)
                },
                onNavigateToAdmin = {
                    navController.navigate(Screen.Admin.route)
                }
            )
        }

        // Student Home (Voice Intake Agent, Multi-app, Zero fields, SAF picker)
        composable(Screen.StudentHome.route) {
            com.childchiller.campuspitch.features.student.StudentHomeScreen(
                onBackToRoleGate = {
                    navController.navigate(Screen.RoleGate.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Investor Home (Voice Search, Ranked Results, Contact Student, Outreach Pipeline)
        composable(Screen.InvestorHome.route) {
            com.childchiller.campuspitch.features.investor.InvestorHomeScreen(
                onBackToRoleGate = {
                    navController.navigate(Screen.RoleGate.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Investor Registration (Voice / Text Registration Agent, Validation & Code Issuance)
        composable(Screen.InvestorRegister.route) {
            com.childchiller.campuspitch.features.investor.InvestorRegisterScreen(
                onBack = { navController.popBackStack() },
                onApprovedCodeIssued = { code ->
                    if (code.isNotBlank()) {
                        navController.navigate(Screen.InvestorHome.route) {
                            popUpTo(Screen.RoleGate.route)
                        }
                    } else {
                        navController.navigate(Screen.InvestorPending.route) {
                            popUpTo(Screen.RoleGate.route)
                        }
                    }
                }
            )
        }

        // Investor Pending (Application Under Review)
        composable(Screen.InvestorPending.route) {
            com.childchiller.campuspitch.features.investor.InvestorPendingScreen(
                onVerified = {
                    navController.navigate(Screen.InvestorHome.route) {
                        popUpTo(Screen.InvestorPending.route) { inclusive = true }
                    }
                },
                onSignOut = {
                    navController.navigate(Screen.RoleGate.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Admin Approval Console (Review & Issue Investor Codes)
        composable(Screen.Admin.route) {
            com.childchiller.campuspitch.features.admin.AdminApprovalScreen(
                onSignOut = {
                    navController.navigate(Screen.RoleGate.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

@Composable
fun PhasePlaceholderScreen(
    title: String,
    subtitle: String,
    buttonLabel: String,
    onButtonClick: () -> Unit
) {
    Scaffold(
        containerColor = DarkBackground,
        bottomBar = { CopyrightFooter() }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = EmeraldLight
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onButtonClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = DeepNavy
                )
            ) {
                Text(buttonLabel)
            }
        }
    }
}
