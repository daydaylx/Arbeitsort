@file:Suppress("LongParameterList", "TooManyFunctions", "MagicNumber")

package de.montagezeit.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.montagezeit.app.ui.theme.MZTokens
import de.montagezeit.app.ui.theme.panelBorderBrush
import de.montagezeit.app.ui.theme.panelColor
import de.montagezeit.app.ui.theme.panelStrongColor

/**
 * Universal screen wrapper with TopBar, Backdrop, optional FAB.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MZScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    MZAppBackdrop {
        Scaffold(
            modifier = modifier,
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (subtitle != null) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = navigationIcon ?: {},
                    actions = actions,
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            floatingActionButton = floatingActionButton,
            snackbarHost = snackbarHost,
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MZHomeShellScaffold(
    title: String,
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    tabIcons: List<ImageVector>? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    MZAppBackdrop {
        Scaffold(
            modifier = modifier,
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        AnimatedContent(
                            targetState = title to subtitle,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(280)) togetherWith
                                    fadeOut(animationSpec = tween(140))
                            },
                            label = "homeShellTitle"
                        ) { (currentTitle, currentSubtitle) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = currentTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                currentSubtitle?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    actions = actions,
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                MZHomeTabBar(
                    tabs = tabs,
                    tabIcons = tabIcons,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = onTabSelected
                )
            },
            content = content
        )
    }
}

/** Solid app background for the quiet dark visual system. */
@Composable
fun MZAppBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        content()
    }
}

@Composable
private fun MZHomeTabBar(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    tabIcons: List<ImageVector>? = null
) {
    val navBarShape = RoundedCornerShape(MZTokens.RadiusCard)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .clip(navBarShape)
            .background(MaterialTheme.colorScheme.panelStrongColor)
            .border(
                width = MZTokens.PanelBorderWidth,
                brush = MaterialTheme.colorScheme.panelBorderBrush,
                shape = navBarShape
            )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            val tabWidth = maxWidth / tabs.size
            val indicatorOffset by animateDpAsState(
                targetValue = tabWidth * selectedTabIndex,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "homeShellTabIndicator"
            )
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(tabWidth)
                    .height(44.dp)
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(MZTokens.RadiusChip))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = MZTokens.AlphaAccentSelected))
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                tabs.forEachIndexed { index, label ->
                    MZTabItem(
                        label = label,
                        icon = tabIcons?.getOrNull(index),
                        isSelected = selectedTabIndex == index,
                        onClick = { onTabSelected(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.MZTabItem(
    label: String,
    icon: ImageVector?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = contentColor
                )
            }
            Text(
                text = label,
                color = contentColor,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

/** Featured intro area per screen. */
@Composable
fun MZHeroPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MZTokens.RadiusHero))
            .background(colorScheme.panelStrongColor)
            .border(
                width = MZTokens.PanelBorderWidth,
                brush = colorScheme.panelBorderBrush,
                shape = RoundedCornerShape(MZTokens.RadiusHero)
            )
            .padding(MZTokens.HeroPadding)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

/**
 * Eyebrow + Title + Supporting Text pattern.
 */
@Composable
fun MZSectionIntro(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = eyebrow.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (supportingText != null) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Standard container for content blocks. */
@Composable
fun MZAppPanel(
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val backgroundColor = if (emphasized) colorScheme.panelStrongColor else colorScheme.panelColor
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MZTokens.RadiusMedium))
            .background(backgroundColor)
            .border(
                width = MZTokens.PanelBorderWidth,
                brush = colorScheme.panelBorderBrush,
                shape = RoundedCornerShape(MZTokens.RadiusMedium)
            )
            .padding(MZTokens.InnerPadding)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MZTokens.ContentSpacing),
            content = content
        )
    }
}

/**
 * Compact numeric/label indicator.
 * label (UPPERCASE, Monospace), value (titleSmall).
 */
@Composable
fun MZMetricChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(MZTokens.RadiusSmall))
            .background(accentColor.copy(alpha = MZTokens.AlphaAccentSurfaceSubtle))
            .border(
                width = MZTokens.PanelBorderWidth,
                color = accentColor.copy(alpha = MZTokens.AlphaAccentBorder),
                shape = RoundedCornerShape(MZTokens.RadiusSmall)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Single-line status badge.
 */
@Composable
fun MZStatusChip(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(MZTokens.RadiusSmall))
            .background(color.copy(alpha = MZTokens.AlphaAccentSurface))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
