package de.montagezeit.app.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.montagezeit.app.R
import de.montagezeit.app.ui.theme.MZTokens
import de.montagezeit.app.ui.screen.edit.EditEntrySheet
import de.montagezeit.app.ui.screen.edit.EditFormData
import de.montagezeit.app.ui.screen.history.HistoryScreen
import de.montagezeit.app.ui.screen.overview.OverviewScreen
import de.montagezeit.app.ui.screen.settings.SettingsScreen
import de.montagezeit.app.ui.screen.today.TodayScreen
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val TOP_BAR_TITLE_FADE_IN_DURATION_MS = 300
private const val TOP_BAR_TITLE_FADE_OUT_DURATION_MS = 150

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MontageZeitNavGraph(
    editRequestDate: String? = null,
    onEditRequestConsumed: (() -> Unit)? = null
) {
    val navController = rememberNavController()

    // Edit Sheet State
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
            val pagerState = rememberPagerState(pageCount = { 3 })
            val coroutineScope = rememberCoroutineScope()

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            AnimatedContent(
                                targetState = pagerState.currentPage,
                                transitionSpec = {
                                    fadeIn(tween(TOP_BAR_TITLE_FADE_IN_DURATION_MS)) togetherWith
                                        fadeOut(tween(TOP_BAR_TITLE_FADE_OUT_DURATION_MS))
                                },
                                label = "topBarTitle"
                            ) { page ->
                                Text(
                                    text = when (page) {
                                        0 -> stringResource(R.string.today_title)
                                        1 -> stringResource(R.string.overview_title)
                                        else -> stringResource(R.string.history_title)
                                    },
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { navController.navigate("settings") }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = stringResource(R.string.settings_title)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                },
                bottomBar = {
                    val navBarShape = RoundedCornerShape(MZTokens.RadiusCard)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .clip(navBarShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.94f))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(
                                    alpha = MZTokens.BorderAlphaNormal
                                ),
                                shape = navBarShape
                            )
                    ) {
                        val tabs = listOf(
                            R.string.today_title,
                            R.string.overview_title,
                            R.string.history_title
                        )
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                        ) {
                            val tabWidth = maxWidth / tabs.size
                            val indicatorOffset by animateDpAsState(
                                targetValue = tabWidth * pagerState.currentPage,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "tab_indicator"
                            )
                            // Sliding pill behind labels
                            Box(
                                modifier = Modifier
                                    .offset(x = indicatorOffset)
                                    .width(tabWidth)
                                    .height(34.dp)
                                    .padding(horizontal = 4.dp)
                                    .clip(RoundedCornerShape(MZTokens.RadiusChip))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            )
                            // Tab labels on top
                            Row(modifier = Modifier.fillMaxWidth()) {
                                tabs.forEachIndexed { index, titleRes ->
                                    val isSelected = pagerState.currentPage == index
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(index)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stringResource(titleRes),
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            ) { paddingValues ->
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
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
                        2 -> HistoryScreen(
                            onOpenEditSheet = { date -> openEditSheet(date) }
                        )
                    }
                }
            }
        }
        composable("settings") {
            SettingsScreen(
                onOpenEditSheet = { date, onDismissed ->
                    openEditSheet(date, onDismissed)
                }
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
