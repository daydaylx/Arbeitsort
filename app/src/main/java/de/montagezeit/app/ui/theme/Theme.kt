package de.montagezeit.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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
    outline = GlassOutline,
    outlineVariant = GlassOutline.copy(alpha = 0.5f),
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
