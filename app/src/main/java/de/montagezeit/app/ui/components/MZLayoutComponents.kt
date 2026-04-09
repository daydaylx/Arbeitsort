package de.montagezeit.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
    const val StatusSurfaceAlpha = 0.10f
    const val NeutralSurfaceAlpha = 0.70f
}

enum class StatusType {
    SUCCESS,
    WARNING,
    ERROR,
    INFO,
    NEUTRAL
}

internal data class StatusPalette(
    val containerColor: Color,
    val accentColor: Color
)

/**
 * Consistent OutlinedTextField colors for the glass dark theme.
 * Focused state stays orange (primary), unfocused border uses neutral slate outline.
 */
@Composable
fun mzOutlinedTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    cursorColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
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
