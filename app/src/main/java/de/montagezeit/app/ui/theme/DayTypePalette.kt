@file:Suppress("MagicNumber")

package de.montagezeit.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import de.montagezeit.app.data.local.entity.DayType

/**
 * Centralized color mapping for day types (WORK, OFF, VACATION, COMP_TIME).
 * Used by calendar cells, history chips, and status badges.
 */
@Immutable
data class DayTypePalette(
    val accent: Color,
    val container: Color,
    val onContainer: Color
)

@Composable
@ReadOnlyComposable
fun dayTypePalette(dayType: DayType): DayTypePalette {
    val colorScheme = MaterialTheme.colorScheme
    return when (dayType) {
        DayType.WORK -> DayTypePalette(
            accent = colorScheme.primary,
            container = colorScheme.surfaceVariant,
            onContainer = colorScheme.onSurface
        )
        DayType.OFF -> DayTypePalette(
            accent = colorScheme.onSurfaceVariant,
            container = colorScheme.secondaryContainer,
            onContainer = colorScheme.onSurface
        )
        DayType.VACATION -> DayTypePalette(
            accent = GlassSuccess,
            container = GlassSuccess.copy(alpha = MZTokens.AlphaAccentSurface),
            onContainer = GlassSuccess
        )
        DayType.COMP_TIME -> DayTypePalette(
            accent = colorScheme.tertiary,
            container = colorScheme.tertiaryContainer,
            onContainer = colorScheme.onSurface
        )
    }
}
