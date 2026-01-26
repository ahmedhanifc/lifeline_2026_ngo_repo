package com.example.responderapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.responderapp.ui.dashboard.DashboardScreen
import com.example.responderapp.ui.login.LoginScreen
import com.example.responderapp.ui.cases.AddCaseScreen
import com.example.responderapp.ui.records.RecordsScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object AddCase : Screen("add_case")
    object Records : Screen("records")
    object PatientDetail : Screen("patient_detail/{caseId}") {
        fun createRoute(caseId: String) = "patient_detail/$caseId"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Login.route) {
        
        // ... previous routes ...
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToAddCase = { navController.navigate(Screen.AddCase.route) },
                onNavigateToRecords = { navController.navigate(Screen.Records.route) }
            )
        }

        composable(Screen.Records.route) {
            RecordsScreen(
                onBack = { navController.popBackStack() },
                onCaseClick = { caseId ->
                    navController.navigate(Screen.PatientDetail.createRoute(caseId))
                }
            )
        }

        composable(
            route = Screen.PatientDetail.route,
            arguments = listOf(
                androidx.navigation.navArgument("caseId") { 
                    type = androidx.navigation.NavType.StringType 
                }
            )
        ) {
            com.example.responderapp.ui.records.PatientDetailScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Add Case Screen Route
        composable(Screen.AddCase.route) {
            com.example.responderapp.ui.cases.AddCaseScreen(
                onCaseSaved = {
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}