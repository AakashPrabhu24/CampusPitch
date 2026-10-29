package com.childchiller.campuspitch.core.navigation

/**
 * Type-safe navigation routes for CampusPitch.
 */
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object RoleGate : Screen("role_gate")
    object StudentHome : Screen("student_home")
    object InvestorHome : Screen("investor_home")
    object InvestorRegister : Screen("investor_register")
    object StudentDashboard : Screen("student_dashboard")
    object InvestorDashboard : Screen("investor_dashboard")
    object InvestorPending : Screen("investor_pending")
    object Admin : Screen("admin")
}
