package com.example.responderapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.responderapp.ui.dashboard.DashboardScreen
import com.example.responderapp.ui.login.LoginScreen
import com.example.responderapp.ui.cases.AddCaseScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object AddCase : Screen("add_case")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Login.route) {
        
        // Login Screen Route
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    // Pop Login off the stack so back button doesn't return to it
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // Dashboard Screen Route
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToAddCase = {
                    navController.navigate(Screen.AddCase.route)
                }
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