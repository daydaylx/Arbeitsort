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
    listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.02f))
)
val ColorScheme.panelColor: Color get() = surface.copy(alpha = 0.60f)
val ColorScheme.panelStrongColor: Color get() = surfaceVariant.copy(alpha = 0.75f)

val ColorScheme.backgroundBrush: Brush get() = Brush.verticalGradient(
    listOf(Color(0xFF08131A), Color(0xFF0B1720), Color(0xFF04080C))
)

val ColorScheme.heroBrush: Brush get() = Brush.linearGradient(
    listOf(Color(0xFF123142), Color(0xFF10222E), Color(0xFF0A141B))
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
    outline = GlassPrimaryGlow,
    outlineVariant = GlassPrimary,
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
