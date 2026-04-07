package de.montagezeit.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.montagezeit.app.ui.theme.GlassError
import de.montagezeit.app.ui.theme.GlassInfo
import de.montagezeit.app.ui.theme.GlassSuccess
import de.montagezeit.app.ui.theme.GlassWarning
import de.montagezeit.app.ui.theme.MZTokens

object AccessibilityDefaults {
    const val MinTouchTargetSize = 48
    val MinTouchTargetSpacing = 8.dp
    val CardPadding = 18.dp
    val CardCornerRadius = MZTokens.RadiusCard
    val ButtonCornerRadius = MZTokens.RadiusButton
    val ButtonHeight = 48.dp
    val PrimaryButtonHeight = 48.dp
    val SecondaryButtonHeight = 44.dp
    val TertiaryButtonHeight = 40.dp
    val IconButtonSize = 48.dp
}

private object GlassLayoutDefaults {
    const val StatusSurfaceAlpha = 0.14f
    const val NeutralSurfaceAlpha = 0.92f
    val HeroPadding = 18.dp
    val HeroContentSpacing = 14.dp
    val HeroTitleSpacing = 4.dp
}

private const val PRESSED_CARD_SCALE = 0.97f
private const val CARD_PRESS_ANIMATION_MS = 100

internal data class StatusPalette(
    val containerColor: Color,
    val accentColor: Color
)

private fun defaultCardShape() = RoundedCornerShape(AccessibilityDefaults.CardCornerRadius)

@Composable
private fun defaultCardColors() = CardDefaults.cardColors(
    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = MZTokens.CardSurfaceAlpha),
    contentColor = MaterialTheme.colorScheme.onSurface
)

@Composable
private fun defaultCardBorder() = BorderStroke(
    width = 1.dp,
    color = MaterialTheme.colorScheme.outline.copy(alpha = MZTokens.BorderAlphaNormal)
)

@Composable
internal fun statusPalette(status: StatusType): StatusPalette = when (status) {
    StatusType.SUCCESS -> StatusPalette(
        containerColor = GlassSuccess.copy(alpha = GlassLayoutDefaults.StatusSurfaceAlpha),
        accentColor = GlassSuccess
    )
    StatusType.WARNING -> StatusPalette(
        containerColor = GlassWarning.copy(alpha = GlassLayoutDefaults.StatusSurfaceAlpha),
        accentColor = GlassWarning
    )
    StatusType.ERROR -> StatusPalette(
        containerColor = GlassError.copy(alpha = GlassLayoutDefaults.StatusSurfaceAlpha),
        accentColor = GlassError
    )
    StatusType.INFO -> StatusPalette(
        containerColor = GlassInfo.copy(alpha = GlassLayoutDefaults.StatusSurfaceAlpha),
        accentColor = GlassInfo
    )
    StatusType.NEUTRAL -> StatusPalette(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = GlassLayoutDefaults.NeutralSurfaceAlpha),
        accentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Seiten-Hintergrund mit tiefdunklem Navy-Grund und zwei Hintergrund-Orbs:
 * Petrol oben-links (primary) und Blau unten-rechts (tertiary).
 * Farblisten werden gecacht um Reallokierungen bei jeder Rekomposition zu vermeiden.
 */
@Composable
fun MZPageBackground(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val bg         = MaterialTheme.colorScheme.background
    val orbPrimary = MaterialTheme.colorScheme.primary
    val orbSecond  = MaterialTheme.colorScheme.tertiary

    val primaryOrbColors = remember(orbPrimary) {
        listOf(orbPrimary.copy(alpha = MZTokens.OrbAlphaPrimary), Color.Transparent)
    }
    val secondaryOrbColors = remember(orbSecond) {
        listOf(orbSecond.copy(alpha = MZTokens.OrbAlphaSecondary), Color.Transparent)
    }

    Box(
        modifier = modifier
            .background(bg)
            .drawBehind {
                drawOrb(
                    colors = primaryOrbColors,
                    center = Offset(
                        size.width  * MZTokens.OrbPrimaryXFraction,
                        size.height * MZTokens.OrbPrimaryYFraction
                    ),
                    radius = MZTokens.OrbPrimaryRadiusDp.toPx()
                )
                drawOrb(
                    colors = secondaryOrbColors,
                    center = Offset(
                        size.width  * MZTokens.OrbSecondaryXFraction,
                        size.height * MZTokens.OrbSecondaryYFraction
                    ),
                    radius = MZTokens.OrbSecondaryRadiusDp.toPx()
                )
            }
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(MZTokens.CardSpacing),
            content = content
        )
    }
}

private fun DrawScope.drawOrb(colors: List<Color>, center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(colors = colors, center = center, radius = radius),
        radius = radius,
        center = center
    )
}

@Composable
fun MZCard(
    modifier: Modifier = Modifier,
    elevation: Dp = MZTokens.CardElevation,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = defaultCardColors(),
        border = defaultCardBorder(),
        shape = defaultCardShape()
    ) {
        Column(
            modifier = Modifier.padding(AccessibilityDefaults.CardPadding),
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MZCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    elevation: Dp = MZTokens.CardElevation,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) PRESSED_CARD_SCALE else 1f,
        animationSpec = tween(CARD_PRESS_ANIMATION_MS),
        label = "cardPress"
    )
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        enabled = enabled,
        interactionSource = interactionSource,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = defaultCardColors(),
        border = defaultCardBorder(),
        shape = defaultCardShape()
    ) {
        Column(
            modifier = Modifier.padding(AccessibilityDefaults.CardPadding),
            content = content
        )
    }
}

enum class StatusType {
    SUCCESS,
    WARNING,
    ERROR,
    INFO,
    NEUTRAL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MZStatusCard(
    title: String,
    status: StatusType,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = statusPalette(status)
    val cardShape = defaultCardShape()
    val cardColors = CardDefaults.cardColors(
        containerColor = palette.containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface
    )
    val cardBorder = BorderStroke(
        1.dp, palette.accentColor.copy(alpha = MZTokens.BorderAlphaEmphasis)
    )
    val cardElevation = CardDefaults.cardElevation(defaultElevation = MZTokens.CardElevation)

    val cardContent: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier.padding(AccessibilityDefaults.CardPadding)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = when (status) {
                        StatusType.SUCCESS -> Icons.Default.CheckCircle
                        StatusType.WARNING -> Icons.Default.Warning
                        StatusType.ERROR   -> Icons.Default.Error
                        StatusType.INFO, StatusType.NEUTRAL -> Icons.Default.Info
                    },
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = palette.accentColor
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            content()
        }
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            enabled = true,
            colors = cardColors,
            elevation = cardElevation,
            border = cardBorder,
            shape = cardShape,
            content = cardContent
        )
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = cardColors,
            elevation = cardElevation,
            border = cardBorder,
            shape = cardShape,
            content = cardContent
        )
    }
}

@Composable
fun MZHeroCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
    badge: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val heroShape = RoundedCornerShape(MZTokens.RadiusHero)
    val borderColor = accentColor?.copy(alpha = MZTokens.BorderAlphaEmphasis)
        ?: MaterialTheme.colorScheme.outline.copy(alpha = MZTokens.BorderAlphaEmphasis)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = heroShape,
        elevation = CardDefaults.cardElevation(defaultElevation = MZTokens.HeroElevation),
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = MZTokens.CardSurfaceAlpha),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .clip(heroShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = MZTokens.CardSurfaceAlpha))
                .then(
                    if (accentColor != null) {
                        Modifier.drawBehind {
                            drawRect(
                                color = accentColor.copy(alpha = 0.9f),
                                topLeft = Offset.Zero,
                                size = Size(4.dp.toPx(), size.height)
                            )
                        }
                    } else {
                        Modifier
                    }
                )
                .padding(GlassLayoutDefaults.HeroPadding),
            verticalArrangement = Arrangement.spacedBy(GlassLayoutDefaults.HeroContentSpacing)
        ) {
            HeroCardHeader(title = title, subtitle = subtitle, badge = badge)

            content()

            action?.invoke()
        }
    }
}

@Composable
private fun HeroCardHeader(
    title: String,
    subtitle: String,
    badge: (@Composable () -> Unit)?
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(GlassLayoutDefaults.HeroTitleSpacing)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        badge?.invoke()
    }
}

/** Glass-styled FilterChip colors for consistent appearance across all screens. */
@Composable
fun glassFilterChipColors(): SelectableChipColors = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
    labelColor = MaterialTheme.colorScheme.onSurface,
    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
)
