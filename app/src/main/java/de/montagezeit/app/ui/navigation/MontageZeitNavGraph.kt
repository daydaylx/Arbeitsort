package de.montagezeit.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.montagezeit.app.ui.screen.edit.EditEntrySheet
import de.montagezeit.app.ui.screen.history.HistoryScreen
import de.montagezeit.app.ui.screen.settings.SettingsScreen
import de.montagezeit.app.ui.screen.today.TodayScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Today : Screen("today", "Heute", Icons.Default.Today)
    object History : Screen("history", "Verlauf", Icons.Default.History)
    object Settings : Screen("settings", "Einstellungen", Icons.Default.Settings)
}

@Composable
fun MontageZeitNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // Edit Sheet State
    var showEditSheet by remember { mutableStateOf(false) }
    var editDate by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf(Screen.Today, Screen.History, Screen.Settings).forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Today.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Today.route) {
                TodayScreen(
                    onOpenEditSheet = { date ->
                        editDate = date.toString()
                        showEditSheet = true
                    }
                )
            }
            
            composable(Screen.History.route) {
                HistoryScreen(
                    onOpenEditSheet = { date ->
                        editDate = date.toString()
                        showEditSheet = true
                    }
                )
            }
            
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
    
    // Edit Modal Bottom Sheet
    if (showEditSheet && editDate != null) {
        EditEntrySheet(
            onDismiss = {
                showEditSheet = false
                editDate = null
            }
        )
    }
}
