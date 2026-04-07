package de.montagezeit.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import de.montagezeit.app.ui.theme.GlassError
import de.montagezeit.app.ui.theme.GlassInfo
import de.montagezeit.app.ui.theme.GlassSuccess
import de.montagezeit.app.ui.theme.GlassWarning

object AccessibilityDefaults {
    const val MinTouchTargetSize = 48
    val MinTouchTargetSpacing = 8.dp
    val CardPadding = 24.dp
    val CardCornerRadius = 32.dp
    val ButtonCornerRadius = 24.dp
    val ButtonHeight = 56.dp
    val PrimaryButtonHeight = 56.dp
    val SecondaryButtonHeight = 56.dp
    val TertiaryButtonHeight = 48.dp
    val IconButtonSize = 48.dp
}

private object GlassLayoutDefaults {
    const val PageTopGlowAlpha = 0.15f
    const val PageCenterGlowAlpha = 0.10f
    val PageGlowRadius = 800.dp
    const val CardSurfaceAlpha = 1.0f
    const val StatusSurfaceAlpha = 0.20f
    const val HeroStartAlpha = 1.0f
    const val HeroEndAlpha = 0.9f
    const val NeutralSurfaceAlpha = 0.8f
    val HeroCornerRadius = 36.dp
    val HeroPadding = 28.dp
    val HeroContentSpacing = 20.dp
    val HeroTitleSpacing = 8.dp
}

internal data class StatusPalette(
    val containerColor: Color,
    val accentColor: Color
)

private fun defaultCardShape() = RoundedCornerShape(AccessibilityDefaults.CardCornerRadius)

@Composable
private fun defaultCardColors() = CardDefaults.cardColors(
    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = GlassLayoutDefaults.CardSurfaceAlpha),
    contentColor = MaterialTheme.colorScheme.onSurface
)

// Removing the border for a softer iOS-like feel. Using elevation instead.
@Composable
private fun defaultCardBorder(): BorderStroke? = null

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

@Composable
fun MZPageBackground(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val base = MaterialTheme.colorScheme.background
    val topGlow = MaterialTheme.colorScheme.primary.copy(alpha = GlassLayoutDefaults.PageTopGlowAlpha)
    val centerGlow = MaterialTheme.colorScheme.tertiary.copy(alpha = GlassLayoutDefaults.PageCenterGlowAlpha)
    val glowRadius = with(LocalDensity.current) { GlassLayoutDefaults.PageGlowRadius.toPx() }

    Box(
        modifier = modifier.drawBehind {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(topGlow, centerGlow, base),
                    center = Offset(size.width / 2f, 0f),
                    radius = glowRadius
                )
            )
        }
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

@Composable
fun MZCard(
    modifier: Modifier = Modifier,
    elevation: Dp = 12.dp, // Increased elevation for softer shadow
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
    elevation: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
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

    val cardBorder: BorderStroke? = null
    val cardElevation = CardDefaults.cardElevation(defaultElevation = 8.dp)

    val cardContent: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier.padding(AccessibilityDefaults.CardPadding)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = when (status) {
                        StatusType.SUCCESS -> Icons.Default.CheckCircle
                        StatusType.WARNING -> Icons.Default.Warning
                        StatusType.ERROR -> Icons.Default.Error
                        StatusType.INFO, StatusType.NEUTRAL -> Icons.Default.Info
                    },
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = palette.accentColor
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = palette.accentColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

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
    badge: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val heroShape = RoundedCornerShape(GlassLayoutDefaults.HeroCornerRadius)
    val backgroundBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = GlassLayoutDefaults.HeroStartAlpha),
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = GlassLayoutDefaults.HeroEndAlpha)
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = heroShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
        border = null,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .clip(heroShape)
                .background(backgroundBrush)
                .padding(GlassLayoutDefaults.HeroPadding),
            verticalArrangement = Arrangement.spacedBy(GlassLayoutDefaults.HeroContentSpacing)
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
                        style = MaterialTheme.typography.headlineLarge, // Increased from headlineSmall
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge, // Increased from bodyMedium
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                    )
                }

                badge?.invoke()
            }

            content()

            action?.invoke()
        }
    }
}
