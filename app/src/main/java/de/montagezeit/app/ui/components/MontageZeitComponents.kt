package de.montagezeit.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.montagezeit.app.R
import de.montagezeit.app.ui.common.PrimaryActionButton
import de.montagezeit.app.ui.common.TertiaryActionButton

/**
 * Accessibility-Konstanten für konsistente Touch-Targets
 */
object AccessibilityDefaults {
    const val MinTouchTargetSize = 48
    val MinTouchTargetSpacing = 8.dp
    val CardPadding = 22.dp
    val CardCornerRadius = 24.dp
    val ButtonCornerRadius = 18.dp
    val ButtonHeight = 54.dp
    val PrimaryButtonHeight = 54.dp
    val SecondaryButtonHeight = 54.dp
    val TertiaryButtonHeight = 48.dp
    val IconButtonSize = 48.dp
}

@Composable
fun MZPageBackground(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val base = MaterialTheme.colorScheme.background
    val topGlow = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    val bottomGlow = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                colors = listOf(topGlow, base, bottomGlow)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

/**
 * Vereinheitlichte Card-Komponente mit besserem Schatten und Padding
 */
@Composable
fun MZCard(
    modifier: Modifier = Modifier,
    elevation: Dp = 2.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(AccessibilityDefaults.CardCornerRadius)
    ) {
        Column(
            modifier = Modifier.padding(AccessibilityDefaults.CardPadding),
            content = content
        )
    }
}

/**
 * Card mit farbigem Header für Status-Anzeigen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MZStatusCard(
    title: String,
    status: StatusType,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val (containerColor, contentColor) = when (status) {
        StatusType.SUCCESS -> MaterialTheme.colorScheme.primaryContainer to
                              MaterialTheme.colorScheme.onPrimaryContainer
        StatusType.WARNING -> MaterialTheme.colorScheme.tertiaryContainer to
                              MaterialTheme.colorScheme.onTertiaryContainer
        StatusType.ERROR -> MaterialTheme.colorScheme.errorContainer to
                            MaterialTheme.colorScheme.onErrorContainer
        StatusType.INFO -> MaterialTheme.colorScheme.secondaryContainer to
                           MaterialTheme.colorScheme.onSecondaryContainer
        StatusType.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant to
                              MaterialTheme.colorScheme.onSurfaceVariant
    }

    val cardShape = RoundedCornerShape(AccessibilityDefaults.CardCornerRadius)
    val cardColors = CardDefaults.cardColors(containerColor = containerColor)

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
                    tint = contentColor
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = contentColor
                )
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(vertical = 10.dp))

            Column {
                content()
            }
        }
    }

    // Use different Card overloads based on onClick
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            enabled = true,
            colors = cardColors,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(
                width = 1.dp,
                color = contentColor.copy(alpha = 0.15f)
            ),
            shape = cardShape,
            content = cardContent
        )
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = cardColors,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(
                width = 1.dp,
                color = contentColor.copy(alpha = 0.15f)
            ),
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
    val backgroundBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.tertiaryContainer
        ),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(backgroundBrush)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                    )
                }

                badge?.invoke()
            }

            content()

            action?.invoke()
        }
    }
}

/**
 * Status-Badge für visuelle Status-Anzeige
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MZStatusBadge(
    text: String,
    type: StatusType,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true
) {
    val (backgroundColor, contentColor) = when (type) {
        StatusType.SUCCESS -> MaterialTheme.colorScheme.primaryContainer to 
                              MaterialTheme.colorScheme.onPrimaryContainer
        StatusType.WARNING -> MaterialTheme.colorScheme.tertiaryContainer to 
                              MaterialTheme.colorScheme.onTertiaryContainer
        StatusType.ERROR -> MaterialTheme.colorScheme.errorContainer to 
                            MaterialTheme.colorScheme.onErrorContainer
        StatusType.INFO -> MaterialTheme.colorScheme.secondaryContainer to 
                           MaterialTheme.colorScheme.onSecondaryContainer
        StatusType.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant to 
                              MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = backgroundColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (showIcon) {
                Icon(
                    imageVector = when (type) {
                        StatusType.SUCCESS -> Icons.Default.CheckCircle
                        StatusType.WARNING -> Icons.Default.Warning
                        StatusType.ERROR -> Icons.Default.Error
                        StatusType.INFO, StatusType.NEUTRAL -> Icons.Default.Info
                    },
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(end = 6.dp)
                        .semantics { invisibleToUser() },
                    tint = contentColor
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor
            )
        }
    }
}

/**
 * Loading-State mit verbessertem Design
 */
@Composable
fun MZLoadingState(
    message: String,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    onCancel: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (progress != null) {
            CircularProgressIndicator(
                progress = progress,
                modifier = Modifier.size(64.dp),
                strokeWidth = 6.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                strokeWidth = 6.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge.copy(
                textAlign = TextAlign.Center
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        onCancel?.let {
            TertiaryActionButton(onClick = it) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    }
}

/**
 * Error-State mit verbessertem Design
 */
@Composable
fun MZErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge.copy(
                textAlign = TextAlign.Center
            ),
            color = MaterialTheme.colorScheme.error
        )

        PrimaryActionButton(
            onClick = onRetry,
            content = { Text(stringResource(R.string.action_retry)) }
        )
    }
}

/**
 * Empty-State für Listen
 */
@Composable
fun MZEmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    action: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.Center
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        action?.let {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(vertical = 8.dp))
            it()
        }
    }
}

/**
 * Section-Header für gruppierte Inhalte
 */
@Composable
fun MZSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        action?.invoke()
    }
}

@Composable
fun MZKeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasize: Boolean = false
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val dotColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        Spacer(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .drawBehind {
                    drawLine(
                        color = dotColor,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()), 0f)
                    )
                }
        )
        Text(
            text = value,
            style = if (emphasize) {
                MaterialTheme.typography.titleSmall
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Info-Row mit Icon und Text
 */
@Composable
fun MZInfoRow(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = iconTint
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )
    }
}

/**
 * Divider mit Label
 */
@Composable
fun MZDividerWithLabel(
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Divider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Divider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

/**
 * Status-Typen für konsistente Farbgebung
 */
enum class StatusType {
    SUCCESS,    // Grün - Erfolg, abgeschlossen
    WARNING,    // Orange - Warnung, Aufmerksamkeit erforderlich
    ERROR,      // Rot - Fehler, Problem
    INFO,       // Blau - Information
    NEUTRAL     // Grau - Neutral, inaktiv
}

/**
 * Extension für klickbare Elemente mit Accessibility
 */
fun Modifier.clickableWithAccessibility(
    onClick: () -> Unit,
    contentDescription: String
): Modifier = this.clickable(
    onClick = onClick,
    onClickLabel = contentDescription
)
