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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.montagezeit.app.ui.theme.MZTokens
import de.montagezeit.app.ui.theme.backgroundBrush
import de.montagezeit.app.ui.theme.glassAccentBorderBrush
import de.montagezeit.app.ui.theme.glassAccentBrush
import de.montagezeit.app.ui.theme.glassHighlightBrush
import de.montagezeit.app.ui.theme.heroBrush
import de.montagezeit.app.ui.theme.panelBorderBrush
import de.montagezeit.app.ui.theme.panelColor
import de.montagezeit.app.ui.theme.panelStrongColor

/**
 * Universal screen wrapper with TopBar, Backdrop, optional FAB.
 * Backdrop uses the background brush and glow circles as per sshterm.
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
                        scrolledContainerColor = Color.Transparent,
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
                        scrolledContainerColor = Color.Transparent,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                MZHomeTabBar(
                    tabs = tabs,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = onTabSelected
                )
            },
            content = content
        )
    }
}

/**
 * Atmospheric background with glow effects and vertical gradient.
 * Matches sshterm specifications: glow circles top-left (220dp) and bottom-right (280dp).
 */
@Composable
fun MZAppBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.backgroundBrush)
    ) {
        // Ambient Glow Orbs for Glassmorphism
        Box(
            modifier = Modifier
                .offset(x = (-100).dp, y = (-80).dp)
                .size(MZTokens.OrbPrimaryRadiusDp * 2)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(colorScheme.primary.copy(alpha = MZTokens.OrbAlphaPrimary), Color.Transparent)
                    )
                )
        )
        
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = 100.dp)
                .size(MZTokens.OrbSecondaryRadiusDp * 2)
                .blur(100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            colorScheme.secondary.copy(alpha = MZTokens.OrbAlphaSecondary),
                            Color.Transparent
                        )
                    )
                )
        )

        // Darkening overlay gradient (Transparent -> Black 0.22)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.22f))
                    )
                )
        )
        content()
    }
}

@Composable
private fun MZHomeTabBar(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
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
                    .height(38.dp)
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(MZTokens.RadiusChip))
                    .background(
                        brush = Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                            )
                        )
                    )
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                tabs.forEachIndexed { index, label ->
                    val isSelected = selectedTabIndex == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onTabSelected(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Featured intro area per screen.
 * Brush background, 1 dp border, 30dp radius, 22dp horizontal / 24dp vertical padding.
 */
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
            .background(colorScheme.heroBrush)
            .drawBehind {
                drawRect(brush = colorScheme.glassHighlightBrush)
            }
            .border(
                width = MZTokens.PanelBorderWidth,
                brush = colorScheme.panelBorderBrush,
                shape = RoundedCornerShape(MZTokens.RadiusHero)
            )
            .padding(horizontal = 22.dp, vertical = 24.dp)
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
            color = MaterialTheme.colorScheme.secondary
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

/**
 * Standard container for content blocks.
 * surface.copy(0.94f), 1 dp border panelBorder, 24dp radius, 20dp padding.
 */
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
            .drawBehind {
                drawRect(brush = colorScheme.glassHighlightBrush)
            }
            .border(
                width = MZTokens.PanelBorderWidth,
                brush = colorScheme.panelBorderBrush,
                shape = RoundedCornerShape(MZTokens.RadiusMedium)
            )
            .padding(20.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
fun MZPanel(
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    MZAppPanel(
        modifier = modifier,
        emphasized = emphasized,
        content = content
    )
}

@Composable
fun MZContentCard(
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    MZAppPanel(
        modifier = modifier,
        emphasized = emphasized,
        content = content
    )
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
            .background(MaterialTheme.colorScheme.glassAccentBrush(accentColor))
            .border(
                width = MZTokens.PanelBorderWidth,
                brush = MaterialTheme.colorScheme.glassAccentBorderBrush(accentColor),
                shape = RoundedCornerShape(MZTokens.RadiusSmall)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = accentColor
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
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
            .background(color.copy(alpha = 0.1f))
            .border(
                width = MZTokens.PanelBorderWidth,
                brush = MaterialTheme.colorScheme.panelBorderBrush,
                shape = RoundedCornerShape(MZTokens.RadiusSmall)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
