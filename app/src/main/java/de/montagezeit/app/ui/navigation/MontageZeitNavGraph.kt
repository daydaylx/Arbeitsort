@file:Suppress("LongMethod")

package de.montagezeit.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.montagezeit.app.R
import de.montagezeit.app.ui.components.MZHomeShellScaffold
import de.montagezeit.app.ui.components.MZScreenScaffold
import de.montagezeit.app.ui.components.TertiaryActionButton
import de.montagezeit.app.ui.screen.edit.EditEntrySheet
import de.montagezeit.app.ui.screen.edit.EditFormData
import de.montagezeit.app.ui.screen.history.HistoryScreen
import de.montagezeit.app.ui.screen.overview.OverviewScreen
import de.montagezeit.app.ui.screen.settings.SettingsScreen
import de.montagezeit.app.ui.screen.today.TodayScreen
import kotlinx.coroutines.launch
import java.time.LocalDate

private data class HomePageChrome(
    val titleRes: Int,
    val subtitleRes: Int
)

private val homePages = listOf(
    HomePageChrome(
        titleRes = R.string.today_title,
        subtitleRes = R.string.home_shell_today_subtitle
    ),
    HomePageChrome(
        titleRes = R.string.overview_title,
        subtitleRes = R.string.home_shell_overview_subtitle
    ),
    HomePageChrome(
        titleRes = R.string.history_title,
        subtitleRes = R.string.home_shell_history_subtitle
    )
)

@Composable
fun MontageZeitNavGraph(
    editRequestDate: String? = null,
    onEditRequestConsumed: (() -> Unit)? = null
) {
    val navController = rememberNavController()

    var showEditSheet by rememberSaveable { mutableStateOf(false) }
    var editDate by rememberSaveable { mutableStateOf<String?>(null) }
    var copiedFormData by remember { mutableStateOf<EditFormData?>(null) }
    var onEditSheetDismissed by remember { mutableStateOf<(() -> Unit)?>(null) }
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

    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("home") {
            val pagerState = rememberPagerState(pageCount = { homePages.size })
            val coroutineScope = rememberCoroutineScope()
            val currentPage = homePages[pagerState.currentPage]

            MZHomeShellScaffold(
                title = stringResource(currentPage.titleRes),
                subtitle = stringResource(currentPage.subtitleRes),
                tabs = homePages.map { stringResource(it.titleRes) },
                selectedTabIndex = pagerState.currentPage,
                onTabSelected = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
                actions = {
                    FilledTonalIconButton(onClick = { navController.navigate("settings") }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_title)
                        )
                    }
                }
            ) { paddingValues: PaddingValues ->
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize(),
                    beyondViewportPageCount = 1,
                    contentPadding = paddingValues
                ) { page ->
                    when (page) {
                        0 -> TodayScreen(
                            onOpenEditSheet = { date -> openEditSheet(date) },
                            onOpenWeekView = {
                                coroutineScope.launch { pagerState.animateScrollToPage(1) }
                            }
                        )

                        1 -> OverviewScreen(
                            onOpenToday = {
                                coroutineScope.launch { pagerState.animateScrollToPage(0) }
                            },
                            onOpenHistory = {
                                coroutineScope.launch { pagerState.animateScrollToPage(2) }
                            },
                            onOpenSettings = { navController.navigate("settings") },
                            onOpenEditSheet = { date -> openEditSheet(date) }
                        )

                        else -> HistoryScreen(
                            onOpenEditSheet = { date -> openEditSheet(date) }
                        )
                    }
                }
            }
        }

        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }

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
