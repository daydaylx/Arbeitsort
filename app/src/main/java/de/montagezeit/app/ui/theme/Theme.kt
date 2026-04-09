@file:Suppress("MagicNumber")

package de.montagezeit.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

import androidx.compose.material3.ColorScheme

/** Semantic Color Aliases as per sshterm */
val ColorScheme.success: Color get() = GlassSuccess
val ColorScheme.warning: Color get() = GlassWarning
val ColorScheme.danger: Color get() = GlassError
val ColorScheme.info: Color get() = GlassInfo
val ColorScheme.panelBorder: Color get() = outline.copy(alpha = 0.5f)
val ColorScheme.panelBorderBrush: Brush get() = Brush.linearGradient(
    colorStops = arrayOf(
        0.0f to Color.White.copy(alpha = MZTokens.GlassBorderAlpha),
        0.35f to Color.White.copy(alpha = 0.10f),
        1.0f to Color.White.copy(alpha = MZTokens.GlassBorderFadeAlpha)
    )
)
val ColorScheme.panelColor: Color get() = surface.copy(alpha = MZTokens.CardSurfaceAlpha)
val ColorScheme.panelStrongColor: Color get() = surfaceVariant.copy(alpha = 0.72f)

val ColorScheme.backgroundBrush: Brush get() = Brush.verticalGradient(
    listOf(Color(0xFF08131A), Color(0xFF0B1720), Color(0xFF04080C))
)

val ColorScheme.heroBrush: Brush get() = Brush.linearGradient(
    listOf(Color(0xFF1A3E52), Color(0xFF112A3A), Color(0xFF0C1C26))
)

val ColorScheme.glassHighlightBrush: Brush get() = Brush.verticalGradient(
    colorStops = arrayOf(
        0.0f to Color.White.copy(alpha = MZTokens.GlassHighlightAlpha),
        0.45f to Color.White.copy(alpha = MZTokens.GlassInnerGlowAlpha),
        1.0f to Color.Transparent
    )
)

val ColorScheme.glassPrimaryButtonBrush: Brush get() = Brush.linearGradient(
    colorStops = arrayOf(
        0.0f to Color.White.copy(alpha = 0.18f),
        0.5f to primary.copy(alpha = 0.40f),
        1.0f to primary.copy(alpha = 0.25f)
    )
)

val ColorScheme.glassPrimaryButtonBorderBrush: Brush get() = Brush.linearGradient(
    colorStops = arrayOf(
        0.0f to Color.White.copy(alpha = 0.35f),
        1.0f to primary.copy(alpha = MZTokens.GlassBorderAlpha)
    )
)

val ColorScheme.glassSelectionBrush: Brush get() = Brush.linearGradient(
    colorStops = arrayOf(
        0.0f to Color.White.copy(alpha = 0.14f),
        0.5f to primary.copy(alpha = 0.32f),
        1.0f to primary.copy(alpha = 0.18f)
    )
)

val ColorScheme.glassSelectionBorderBrush: Brush get() = Brush.linearGradient(
    colorStops = arrayOf(
        0.0f to Color.White.copy(alpha = 0.25f),
        1.0f to primary.copy(alpha = 0.20f)
    )
)

fun ColorScheme.glassAccentBrush(accentColor: Color): Brush = Brush.linearGradient(
    colorStops = arrayOf(
        0.0f to Color.White.copy(alpha = MZTokens.GlassInnerGlowAlpha),
        0.5f to accentColor.copy(alpha = 0.18f),
        1.0f to accentColor.copy(alpha = 0.10f)
    )
)

fun ColorScheme.glassAccentBorderBrush(accentColor: Color): Brush = Brush.linearGradient(
    colorStops = arrayOf(
        0.0f to Color.White.copy(alpha = 0.24f),
        1.0f to accentColor.copy(alpha = 0.22f)
    )
)

private val GlassColorScheme = darkColorScheme(
    primary = GlassPrimary,
    onPrimary = GlassOnDark,
    primaryContainer = GlassPrimaryContainer,
    onPrimaryContainer = GlassOnDark,
    secondary = GlassSecondary,
    onSecondary = GlassOnDark,
    secondaryContainer = GlassSecondaryContainer,
    onSecondaryContainer = GlassSecondary,
    tertiary = GlassTertiary,
    onTertiary = GlassOnDark,
    tertiaryContainer = GlassTertiaryContainer,
    onTertiaryContainer = GlassTertiary,
    error = GlassError,
    onError = GlassOnDark,
    errorContainer = GlassErrorContainer,
    onErrorContainer = GlassError,
    background = GlassBackground,
    onBackground = GlassOnDark,
    surface = GlassSurface,
    onSurface = GlassOnDark,
    surfaceVariant = GlassSurfaceVariant,
    onSurfaceVariant = GlassOnDarkMuted,
    outline = Color(0xFF3A4A5C),         // Neutral slate — prevents orange bleed into borders/dividers
    outlineVariant = Color(0xFF1E2D3D),  // Dark slate for HorizontalDivider
    inverseSurface = GlassOnDark,
    inverseOnSurface = GlassBackground,
    inversePrimary = GlassPrimaryContainer,
    scrim = GlassBackground
)

@Composable
fun MontageZeitTheme(content: @Composable () -> Unit) {
    val colorScheme = GlassColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        @Suppress("DEPRECATION")
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
