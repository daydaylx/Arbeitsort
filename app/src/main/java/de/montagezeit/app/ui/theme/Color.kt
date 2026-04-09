@file:Suppress("MagicNumber")

package de.montagezeit.app.ui.theme

import androidx.compose.ui.graphics.Color

// Semantic palette for the app's fixed dark theme.
val GlassBackground     = Color(0xFF060D15)
val GlassSurface        = Color(0xFF0C1622)
val GlassSurfaceVariant = Color(0xFF121E2E)

// Primary: Warm Orange/Amber
val GlassPrimary          = Color(0xFFFF9800)
val GlassPrimaryContainer = Color(0xFF4E2C00)
val GlassPrimaryGlow      = Color(0xFFFFB74D)

// Secondary: Teal-Mint — distinct from primary orange, complementary to tertiary blue
val GlassSecondary          = Color(0xFF80CBC4)
val GlassSecondaryContainer = Color(0xFF00352F)

// Tertiary: cool blue for alternate informational accents
val GlassTertiary          = Color(0xFF99BDF0)
val GlassTertiaryContainer = Color(0xFF17344F)

// Text
val GlassOnDark      = Color(0xFFF5F2F0) // Warm ivory
val GlassOnDarkMuted = Color(0xFFC8C3C0) // Warm gray

// Status / Semantic Palette
val Mint500  = Color(0xFF37E3A5)
val Cyan500  = Color(0xFF43B9FF)
val Yellow500 = Color(0xFFFFB300) // Amber — professional warning tone, less harsh than pure yellow
val Rose500  = Color(0xFFFF6F7D)

// Status Aliases
val GlassError   = Rose500
val GlassWarning = Yellow500
val GlassSuccess = Mint500
val GlassInfo    = Cyan500

val GlassErrorContainer = Color(0xFF4D000E)
