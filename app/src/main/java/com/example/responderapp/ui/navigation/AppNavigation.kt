package com.example.responderapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.responderapp.ui.dashboard.DashboardScreen
import com.example.responderapp.ui.login.LoginScreen
import com.example.responderapp.ui.cases.AddCaseScreen
import com.example.responderapp.ui.records.RecordsScreen
import com.example.responderapp.ui.records.PatientDetailScreen
import com.example.responderapp.ui.cases.EditCaseScreen

sealed class Screen(val route: String, val label: String = "", val icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard", "Home", Icons.Default.Home)
    object AddCase : Screen("add_case")
    object Records : Screen("records", "Records", Icons.Outlined.Description)
    object PatientDetail : Screen("patient_detail/{caseId}") {
        fun createRoute(caseId: String) = "patient_detail/$caseId"
    }
    object EditCase : Screen("edit_case/{caseId}") {
        fun createRoute(caseId: String) = "edit_case/$caseId"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val mainItems = listOf(
        Screen.Dashboard,
        Screen.Records
    )

    val showBottomBar = currentDestination?.route in mainItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Color.White,
                    contentColor = Color(0xFF3B6EB4)
                ) {
                    mainItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { 
                                screen.icon?.let { 
                                    Icon(it, contentDescription = screen.label) 
                                } 
                            },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                if (screen.route != "") {
                                    navController.navigate(screen.route) {
                                        // Pop up to the start destination of the graph to
                                        // avoid building up a large stack of destinations
                                        // on the back stack as users select items
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        // Avoid multiple copies of the same destination when
                                        // reselecting the same item
                                        launchSingleTop = true
                                        // Restore state when reselecting a previously selected item
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController, 
            startDestination = Screen.Login.route,
            modifier = Modifier.padding(innerPadding)
        ) {
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
                    onNavigateToRecords = { navController.navigate(Screen.Records.route) },
                    onNavigateToDetail = { caseId -> 
                        navController.navigate(Screen.PatientDetail.createRoute(caseId))
                    }
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
                PatientDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { caseId ->
                        navController.navigate(Screen.EditCase.createRoute(caseId))
                    }
                )
            }

            composable(
                route = Screen.EditCase.route,
                arguments = listOf(
                    androidx.navigation.navArgument("caseId") { 
                        type = androidx.navigation.NavType.StringType 
                    }
                )
            ) {
                EditCaseScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }

            composable(Screen.AddCase.route) {
                AddCaseScreen(
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
}
