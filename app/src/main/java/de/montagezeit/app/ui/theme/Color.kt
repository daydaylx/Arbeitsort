@file:Suppress("MagicNumber")

package de.montagezeit.app.ui.theme

import androidx.compose.ui.graphics.Color

// Semantic palette for the app's fixed dark theme.
val GlassBackground     = Color(0xFF04080F)
val GlassSurface        = Color(0xFF09111D)
val GlassSurfaceVariant = Color(0xFF0F1826)

// Primary: Warm Orange/Amber
val GlassPrimary          = Color(0xFFFF9800)
val GlassPrimaryContainer = Color(0xFF4E2C00)
val GlassPrimaryGlow      = Color(0xFFFFB74D)

// Secondary: Warm neutral surface accent
val GlassSecondary          = Color(0xFFFFCCBC)
val GlassSecondaryContainer = Color(0xFF3E2723)

// Tertiary: cool blue for alternate informational accents
val GlassTertiary          = Color(0xFF99BDF0)
val GlassTertiaryContainer = Color(0xFF17344F)

// Text
val GlassOnDark      = Color(0xFFF5F2F0) // Warm ivory
val GlassOnDarkMuted = Color(0xFFC8C3C0) // Warm gray

// Status / Semantic Palette
val Mint500  = Color(0xFF37E3A5)
val Cyan500  = Color(0xFF43B9FF)
val Yellow500 = Color(0xFFFFEB3B) // Clear yellow for warning, replacing amber to not clash with orange
val Rose500  = Color(0xFFFF6F7D)

// Status Aliases
val GlassError   = Rose500
val GlassWarning = Yellow500
val GlassSuccess = Mint500
val GlassInfo    = Cyan500

val GlassErrorContainer = Color(0xFF4D000E)
