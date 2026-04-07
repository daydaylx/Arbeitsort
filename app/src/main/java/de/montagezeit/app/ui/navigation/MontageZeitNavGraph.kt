package de.montagezeit.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import de.montagezeit.app.ui.components.MZTokens
import de.montagezeit.app.ui.theme.DeepNavy
import de.montagezeit.app.ui.theme.Navy
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import de.montagezeit.app.ui.screen.overview.OverviewScreen
import de.montagezeit.app.ui.screen.settings.SettingsScreen
import de.montagezeit.app.ui.screen.today.TodayScreen
import java.time.LocalDate

sealed class Screen(val route: String, @StringRes val labelRes: Int, val icon: ImageVector) {
    object Today : Screen("today", R.string.today_title, Icons.Default.Today)
    object Overview : Screen("overview", R.string.overview_title, Icons.Default.Dashboard)
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
    var showEditSheet by rememberSaveable { mutableStateOf(false) }
    var editDate by rememberSaveable { mutableStateOf<String?>(null) }
    var copiedFormData by remember { mutableStateOf<EditFormData?>(null) }      // transient, kein Saveable nötig
    var onEditSheetDismissed by remember { mutableStateOf<(() -> Unit)?>(null) } // Lambda, kein Saveable
    val currentOnEditSheetDismissed by rememberUpdatedState(onEditSheetDismissed)

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
            // Hintergrund: tiefes Navy, nahtlos mit dem Screen-Hintergrund
            Surface(
                tonalElevation = 0.dp,
                color = DeepNavy
            ) {
                // Glassmorphism Pill-Container
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .navigationBarsPadding(),
                    shape = RoundedCornerShape(MZTokens.RadiusCard),
                    color = Navy.copy(alpha = 0.90f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = MZTokens.BorderAlphaNormal)
                    )
                ) {
                    NavigationBar(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        tonalElevation = 0.dp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        listOf(Screen.Today, Screen.Overview, Screen.History, Screen.Settings).forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = null) },
                                label = { Text(stringResource(screen.labelRes)) },
                                alwaysShowLabel = true,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor   = MaterialTheme.colorScheme.primary,
                                    selectedTextColor   = MaterialTheme.colorScheme.primary,
                                    indicatorColor      = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
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
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Today.route,
            modifier = Modifier.padding(paddingValues),
            enterTransition    = { fadeIn(animationSpec = tween(durationMillis = 220)) },
            exitTransition     = { fadeOut(animationSpec = tween(durationMillis = 180)) },
            popEnterTransition = { fadeIn(animationSpec = tween(durationMillis = 220)) },
            popExitTransition  = { fadeOut(animationSpec = tween(durationMillis = 180)) }
        ) {
            composable(Screen.Today.route) {
                TodayScreen(
                    onOpenEditSheet = { date ->
                        openEditSheet(date)
                    },
                    onOpenWeekView = {
                        navController.navigate(Screen.Overview.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.Overview.route) {
                OverviewScreen(
                    onOpenToday = {
                        navController.navigate(Screen.Today.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenHistory = {
                        navController.navigate(Screen.History.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenSettings = {
                        navController.navigate(Screen.Settings.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenEditSheet = { date ->
                        openEditSheet(date)
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
                SettingsScreen(
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
                currentOnEditSheetDismissed?.invoke()
                onEditSheetDismissed = null
            },
            onCopyToNewDate = { newDate, formData ->
                // Sheet bleibt offen — Datum + Daten aktualisieren ohne Flash
                editDate = newDate.toString()
                copiedFormData = formData
                onEditSheetDismissed = null
            },
            onNavigateDate = { newDate ->
                editDate = newDate.toString()
                copiedFormData = null
            }
        )
    }
}
