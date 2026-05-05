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
val ColorScheme.panelBorder: Color get() = outline.copy(alpha = MZTokens.BorderAlphaNormal)
val ColorScheme.panelBorderBrush: Brush get() = Brush.linearGradient(
    listOf(panelBorder, panelBorder)
)
val ColorScheme.panelColor: Color get() = surface.copy(alpha = MZTokens.CardSurfaceAlpha)
val ColorScheme.panelStrongColor: Color get() = surfaceVariant

private val GlassColorScheme = darkColorScheme(
    primary = GlassPrimary,
    onPrimary = GlassOnAccent,
    primaryContainer = GlassPrimaryContainer,
    onPrimaryContainer = GlassOnDark,
    secondary = GlassSecondary,
    onSecondary = GlassOnAccent,
    secondaryContainer = GlassSecondaryContainer,
    onSecondaryContainer = GlassOnDark,
    tertiary = GlassTertiary,
    onTertiary = GlassOnAccent,
    tertiaryContainer = GlassTertiaryContainer,
    onTertiaryContainer = GlassOnDark,
    error = GlassError,
    onError = GlassOnAccent,
    errorContainer = GlassErrorContainer,
    onErrorContainer = GlassError,
    background = GlassBackground,
    onBackground = GlassOnDark,
    surface = GlassSurface,
    onSurface = GlassOnDark,
    surfaceVariant = GlassSurfaceVariant,
    onSurfaceVariant = GlassOnDarkMuted,
    outline = Color(0xFF4B5560),
    outlineVariant = Color(0xFF303942),
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
