package de.montagezeit.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import de.montagezeit.app.R
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.montagezeit.app.ui.screen.edit.EditEntrySheet
import de.montagezeit.app.ui.screen.edit.EditFormData
import de.montagezeit.app.ui.screen.history.HistoryScreen
import de.montagezeit.app.ui.screen.settings.SettingsScreenV2
import de.montagezeit.app.ui.screen.today.TodayScreenV2
import java.time.LocalDate

sealed class Screen(val route: String, @StringRes val labelRes: Int, val icon: ImageVector) {
    object Today : Screen("today", R.string.today_title, Icons.Default.Today)
    object History : Screen("history", R.string.history_title, Icons.Default.History)
    object Settings : Screen("settings", R.string.settings_title, Icons.Default.Settings)
}

@Composable
fun MontageZeitNavGraph(
    editRequestDate: String? = null,
    onEditRequestConsumed: (() -> Unit)? = null
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // Edit Sheet State
    var showEditSheet by remember { mutableStateOf(false) }
    var editDate by remember { mutableStateOf<String?>(null) }
    var copiedFormData by remember { mutableStateOf<EditFormData?>(null) }
    var onEditSheetDismissed by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun openEditSheet(date: LocalDate, onDismissed: (() -> Unit)? = null) {
        editDate = date.toString()
        showEditSheet = true
        onEditSheetDismissed = onDismissed
    }

    LaunchedEffect(editRequestDate) {
        if (editRequestDate != null) {
            openEditSheet(LocalDate.parse(editRequestDate))
            onEditRequestConsumed?.invoke()
        }
    }
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf(Screen.Today, Screen.History, Screen.Settings).forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(stringResource(screen.labelRes)) },
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
                TodayScreenV2(
                    onOpenEditSheet = { date ->
                        openEditSheet(date)
                    },
                    onOpenWeekView = {
                        navController.navigate(Screen.History.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            
            composable(Screen.History.route) {
                HistoryScreen(
                    onOpenEditSheet = { date ->
                        openEditSheet(date)
                    }
                )
            }
            
            composable(Screen.Settings.route) {
                SettingsScreenV2(
                    onOpenEditSheet = { date, onDismissed ->
                        openEditSheet(date, onDismissed)
                    }
                )
            }
        }
    }
    
    // Edit Modal Bottom Sheet
    if (showEditSheet && editDate != null) {
        EditEntrySheet(
            date = LocalDate.parse(editDate),
            initialFormData = copiedFormData,
            onDismiss = {
                showEditSheet = false
                editDate = null
                copiedFormData = null
                onEditSheetDismissed?.invoke()
                onEditSheetDismissed = null
            },
            onCopyToNewDate = { newDate, formData ->
                // Schließe aktuelles Sheet und öffne neues mit kopierten Daten
                showEditSheet = false
                editDate = newDate.toString()
                copiedFormData = formData
                showEditSheet = true
            },
            onNavigateDate = { newDate ->
                editDate = newDate.toString()
                copiedFormData = null
            }
        )
    }
}
