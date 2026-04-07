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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.montagezeit.app.R
import de.montagezeit.app.ui.theme.Blue30
import de.montagezeit.app.ui.theme.Blue40
import de.montagezeit.app.ui.theme.PetrolGlow
import de.montagezeit.app.ui.theme.Teal30
import de.montagezeit.app.ui.theme.TealDeep


// ─────────────────────────────────────────────────────────────────────────────
//  Design Tokens
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Zentrale Design-Token für das Premium Dark Control Dashboard.
 */
object MZTokens {
    // Radius
    val RadiusCard   = 28.dp
    val RadiusHero   = 32.dp
    val RadiusButton = 50.dp   // Pill
    val RadiusBadge  = 50.dp   // Pill
    val RadiusChip   = 20.dp
    val RadiusModal  = 32.dp

    // Glass
    val GlassAlpha       = 0.10f   // Card-Hintergrund-Alpha (erhöht auf 10 % für besseren Kontrast)
    val GlassBorderAlpha = 0.18f   // Standard Leucht-Kontur (veraltet → BorderAlphaNormal bevorzugen)
    val GlowBorderAlpha  = 0.45f   // Hero / aktive Leucht-Kontur (veraltet → BorderAlphaEmphasis bevorzugen)

    // Semantische Border-Alphas (vereinheitlicht für konsistente Hierarchie)
    val BorderAlphaSubtle   = 0.12f  // Dezente Kontur – Divider, passive Elemente
    val BorderAlphaNormal   = 0.22f  // Standard-Kontur – Karten, Nav-Bar
    val BorderAlphaEmphasis = 0.45f  // Akzentuierte Kontur – Hero-Karte, aktive Elemente

    // Background Orbs – als Konstanten statt Magic Numbers
    val OrbAlphaPrimary       = 0.14f
    val OrbAlphaSecondary     = 0.07f
    val OrbPrimaryXFraction   = 0.15f
    val OrbPrimaryYFraction   = 0.12f
    val OrbSecondaryXFraction = 0.88f
    val OrbSecondaryYFraction = 0.82f
    val OrbPrimaryRadiusDp    = 320.dp
    val OrbSecondaryRadiusDp  = 220.dp

    // Spacing
    val ScreenPadding = 20.dp
    val CardSpacing   = 14.dp
    val InnerPadding  = 20.dp

    // Elevation
    val CardElevation = 0.dp
    val HeroElevation = 2.dp
}

// ─────────────────────────────────────────────────────────────────────────────
//  Accessibility Defaults
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Accessibility-Konstanten für konsistente Touch-Targets
 */
object AccessibilityDefaults {
    const val MinTouchTargetSize    = 48
    val MinTouchTargetSpacing       = 8.dp
    val CardPadding                 = 20.dp
    val CardCornerRadius            = 28.dp
    val ButtonCornerRadius          = 50.dp  // Pill
    val ButtonHeight                = 54.dp
    val PrimaryButtonHeight         = 54.dp
    val SecondaryButtonHeight       = 54.dp
    val TertiaryButtonHeight        = 48.dp
    val IconButtonSize              = 48.dp
}

// ─────────────────────────────────────────────────────────────────────────────
//  Background
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Seiten-Hintergrund mit tiefdunklem Navy-Gradient und zwei Hintergrund-Orbs
 * (oben-links: Petrol; unten-rechts: Blau) für die Dashboard-Tiefenwirkung.
 */
@Composable
fun MZPageBackground(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val bg         = MaterialTheme.colorScheme.background
    val orbPrimary = MaterialTheme.colorScheme.primary
    val orbBlue    = MaterialTheme.colorScheme.tertiary

    // Farblisten werden gecacht um Neu-Allokierungen bei jeder Rekomposition zu vermeiden
    val primaryOrbColors   = remember(orbPrimary) {
        listOf(orbPrimary.copy(alpha = MZTokens.OrbAlphaPrimary), Color.Transparent)
    }
    val secondaryOrbColors = remember(orbBlue) {
        listOf(orbBlue.copy(alpha = MZTokens.OrbAlphaSecondary), Color.Transparent)
    }

    Box(
        modifier = modifier
            .background(bg)
            .drawBehind {
                val primaryRadius   = MZTokens.OrbPrimaryRadiusDp.toPx()
                val secondaryRadius = MZTokens.OrbSecondaryRadiusDp.toPx()
                drawOrb(
                    colors = primaryOrbColors,
                    center = Offset(size.width * MZTokens.OrbPrimaryXFraction, size.height * MZTokens.OrbPrimaryYFraction),
                    radius = primaryRadius
                )
                drawOrb(
                    colors = secondaryOrbColors,
                    center = Offset(size.width * MZTokens.OrbSecondaryXFraction, size.height * MZTokens.OrbSecondaryYFraction),
                    radius = secondaryRadius
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

private fun DrawScope.drawOrb(colors: List<Color>, center: Offset, radius: Float) {
    drawCircle(
        brush  = Brush.radialGradient(colors = colors, center = center, radius = radius),
        radius = radius,
        center = center
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Cards
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Glass-Karte — transparenter Hintergrund mit leuchtender Petrol-Kontur.
 */
@Composable
fun MZCard(
    modifier: Modifier = Modifier,
    elevation: Dp = MZTokens.CardElevation,
    content: @Composable ColumnScope.() -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(
            containerColor = primary.copy(alpha = MZTokens.GlassAlpha)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = primary.copy(alpha = MZTokens.BorderAlphaNormal)
        ),
        shape = RoundedCornerShape(MZTokens.RadiusCard)
    ) {
        Column(
            modifier = Modifier.padding(AccessibilityDefaults.CardPadding),
            content = content
        )
    }
}

/**
 * Klickbare Glass-Karte.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MZCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    elevation: Dp = MZTokens.CardElevation,
    content: @Composable ColumnScope.() -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(
            containerColor = primary.copy(alpha = MZTokens.GlassAlpha)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = primary.copy(alpha = MZTokens.BorderAlphaNormal)
        ),
        shape = RoundedCornerShape(MZTokens.RadiusCard)
    ) {
        Column(
            modifier = Modifier.padding(AccessibilityDefaults.CardPadding),
            content = content
        )
    }
}

/**
 * Status-Karte mit alpha-basierter Statusfarbe und leuchtender Kontur.
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
    val (baseColor, contentColor) = when (status) {
        StatusType.SUCCESS -> MaterialTheme.colorScheme.primary         to MaterialTheme.colorScheme.onPrimaryContainer
        StatusType.WARNING -> MaterialTheme.colorScheme.tertiary        to MaterialTheme.colorScheme.onTertiaryContainer
        StatusType.ERROR   -> MaterialTheme.colorScheme.error           to MaterialTheme.colorScheme.onErrorContainer
        StatusType.INFO    -> MaterialTheme.colorScheme.secondary       to MaterialTheme.colorScheme.onSecondaryContainer
        StatusType.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    val cardShape  = RoundedCornerShape(MZTokens.RadiusCard)
    val cardColors = CardDefaults.cardColors(containerColor = baseColor.copy(alpha = 0.12f))
    val cardBorder = BorderStroke(1.dp, baseColor.copy(alpha = MZTokens.BorderAlphaEmphasis))

    val cardContent: @Composable ColumnScope.() -> Unit = {
        Column(modifier = Modifier.padding(AccessibilityDefaults.CardPadding)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = when (status) {
                        StatusType.SUCCESS -> Icons.Default.CheckCircle
                        StatusType.WARNING -> Icons.Default.Warning
                        StatusType.ERROR   -> Icons.Default.Error
                        StatusType.INFO, StatusType.NEUTRAL -> Icons.Default.Info
                    },
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = contentColor
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = contentColor
                )
            }
            Spacer(modifier = Modifier.padding(vertical = 10.dp))
            Column { content() }
        }
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            enabled = true,
            colors = cardColors,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = cardBorder,
            shape = cardShape,
            content = cardContent
        )
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = cardColors,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = cardBorder,
            shape = cardShape,
            content = cardContent
        )
    }
}

/**
 * Hero-Karte mit diagonalem Teal→Blue-Gradient und leuchtender Glow-Kontur.
 */
@Composable
fun MZHeroCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    badge: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val heroBrush = Brush.linearGradient(
        colors = listOf(Teal30, Blue40),  // Etwas heller als TealDeep→Blue30 für bessere Sichtbarkeit
        start  = Offset(0f, 0f),
        end    = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )
    val primary = MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MZTokens.RadiusHero),
        elevation = CardDefaults.cardElevation(defaultElevation = MZTokens.HeroElevation),
        border = BorderStroke(1.5.dp, primary.copy(alpha = MZTokens.BorderAlphaEmphasis)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(MZTokens.RadiusHero))
                .background(heroBrush)
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

// ─────────────────────────────────────────────────────────────────────────────
//  Badge
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Pillenförmiges Status-Badge.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MZStatusBadge(
    text: String,
    type: StatusType,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true
) {
    val (bgColor, contentColor) = when (type) {
        StatusType.SUCCESS -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)   to MaterialTheme.colorScheme.primary
        StatusType.WARNING -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)  to MaterialTheme.colorScheme.tertiary
        StatusType.ERROR   -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)     to MaterialTheme.colorScheme.error
        StatusType.INFO    -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) to MaterialTheme.colorScheme.secondary
        StatusType.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant                to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = bgColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(MZTokens.RadiusBadge),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.30f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            if (showIcon) {
                Icon(
                    imageVector = when (type) {
                        StatusType.SUCCESS -> Icons.Default.CheckCircle
                        StatusType.WARNING -> Icons.Default.Warning
                        StatusType.ERROR   -> Icons.Default.Error
                        StatusType.INFO, StatusType.NEUTRAL -> Icons.Default.Info
                    },
                    contentDescription = null,
                    modifier = Modifier
                        .size(14.dp)
                        .semantics { invisibleToUser() },
                    tint = contentColor
                )
                Spacer(modifier = Modifier.size(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  State Screens
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Loading-State
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
                progress = { progress },
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
            style = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
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
 * Error-State
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
            style = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
            color = MaterialTheme.colorScheme.error
        )
        PrimaryActionButton(
            onClick = onRetry,
            content = { Text(stringResource(R.string.action_retry)) }
        )
    }
}

/**
 * Empty-State
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
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        modifier = Modifier.size(38.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
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
            style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        action?.let {
            Spacer(modifier = Modifier.padding(vertical = 8.dp))
            it()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Layout Helpers
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Abschnittstitel mit optionalem Aktions-Link.
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
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        action?.invoke()
    }
}

/**
 * Label–Wert-Zeile mit gepunkteter Trennlinie (Petrol-getönt).
 */
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
        val dotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
        Spacer(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .drawBehind {
                    drawLine(
                        color = dotColor,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(4.dp.toPx(), 4.dp.toPx()), 0f
                        )
                    )
                }
        )
        Text(
            text = value,
            style = if (emphasize) MaterialTheme.typography.titleSmall
                    else            MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End
        )
    }
}

/**
 * Icon + Text-Zeile.
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
 * Trennlinie mit zentriertem Label (Teal-Tönung).
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
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Status Type
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Status-Typen für konsistente Farbgebung.
 */
enum class StatusType {
    SUCCESS,    // Grün/Teal — Erfolg
    WARNING,    // Orange — Achtung
    ERROR,      // Rot — Fehler
    INFO,       // Blau — Information
    NEUTRAL     // Grau — Inaktiv
}

// ─────────────────────────────────────────────────────────────────────────────
//  Modifier Extension
// ─────────────────────────────────────────────────────────────────────────────

fun Modifier.clickableWithAccessibility(
    onClick: () -> Unit,
    contentDescription: String
): Modifier = this.clickable(
    onClick = onClick,
    onClickLabel = contentDescription
)

// ─────────────────────────────────────────────────────────────────────────────
//  Buttons
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Primärer Pill-Button (solid Teal).
 */
@Composable
fun PrimaryActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    contentDescription: String? = null,
    minHeight: Dp = AccessibilityDefaults.PrimaryButtonHeight,
    shape: androidx.compose.ui.graphics.Shape? = null,
    colors: androidx.compose.material3.ButtonColors? = null,
    content: @Composable RowScope.() -> Unit
) {
    val semanticsMod = if (!contentDescription.isNullOrBlank()) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else Modifier
    val resolvedShape  = shape  ?: RoundedCornerShape(AccessibilityDefaults.ButtonCornerRadius)
    val resolvedColors = colors ?: androidx.compose.material3.ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor   = MaterialTheme.colorScheme.onPrimary
    )
    androidx.compose.material3.Button(
        onClick  = onClick,
        modifier = semanticsMod.then(modifier).heightIn(min = minHeight),
        enabled  = enabled && !isLoading,
        shape    = resolvedShape,
        colors   = resolvedColors
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier   = Modifier.size(18.dp).padding(end = 8.dp),
                strokeWidth = 2.dp,
                color       = androidx.compose.material3.LocalContentColor.current
            )
        } else {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
        content()
    }
}

/**
 * Sekundärer Pill-Button (Outlined, Teal-Kontur).
 */
@Composable
fun SecondaryActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    contentDescription: String? = null,
    minHeight: Dp = AccessibilityDefaults.SecondaryButtonHeight,
    shape: androidx.compose.ui.graphics.Shape? = null,
    colors: androidx.compose.material3.ButtonColors? = null,
    content: @Composable RowScope.() -> Unit
) {
    val semanticsMod = if (!contentDescription.isNullOrBlank()) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else Modifier
    val resolvedShape  = shape  ?: RoundedCornerShape(AccessibilityDefaults.ButtonCornerRadius)
    val resolvedColors = colors ?: androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
        contentColor = MaterialTheme.colorScheme.primary
    )
    androidx.compose.material3.OutlinedButton(
        onClick      = onClick,
        modifier     = semanticsMod.then(modifier).heightIn(min = minHeight),
        enabled      = enabled && !isLoading,
        shape        = resolvedShape,
        colors       = resolvedColors,
        border       = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.50f))
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier    = Modifier.size(18.dp).padding(end = 8.dp),
                strokeWidth = 2.dp
            )
        } else {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
        content()
    }
}

/**
 * Tertiärer Pill-Button (Text).
 */
@Composable
fun TertiaryActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    contentDescription: String? = null,
    minHeight: Dp = AccessibilityDefaults.TertiaryButtonHeight,
    shape: androidx.compose.ui.graphics.Shape? = null,
    colors: androidx.compose.material3.ButtonColors? = null,
    content: @Composable RowScope.() -> Unit
) {
    val semanticsMod = if (!contentDescription.isNullOrBlank()) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else Modifier
    val resolvedShape  = shape  ?: RoundedCornerShape(AccessibilityDefaults.ButtonCornerRadius)
    val resolvedColors = colors ?: androidx.compose.material3.ButtonDefaults.textButtonColors()
    androidx.compose.material3.TextButton(
        onClick  = onClick,
        modifier = semanticsMod.then(modifier).heightIn(min = minHeight),
        enabled  = enabled && !isLoading,
        shape    = resolvedShape,
        colors   = resolvedColors
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier    = Modifier.size(18.dp).padding(end = 8.dp),
                strokeWidth = 2.dp
            )
        } else {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
        content()
    }
}

/**
 * Destruktiver Pill-Button (Error-Rot).
 */
@Composable
fun DestructiveActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    androidx.compose.material3.Button(
        onClick  = onClick,
        modifier = modifier.heightIn(min = AccessibilityDefaults.PrimaryButtonHeight),
        enabled  = enabled,
        shape    = RoundedCornerShape(AccessibilityDefaults.ButtonCornerRadius),
        colors   = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor   = MaterialTheme.colorScheme.onError
        ),
        content = content
    )
}
