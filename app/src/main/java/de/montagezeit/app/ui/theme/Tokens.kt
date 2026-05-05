@file:Suppress("MagicNumber")

package de.montagezeit.app.ui.theme

import androidx.compose.ui.unit.dp

object MZTokens {
    // Radius
    val RadiusExtraSmall = 6.dp
    val RadiusSmall      = 8.dp
    val RadiusMedium     = 12.dp
    val RadiusLarge      = 16.dp
    val RadiusExtraLarge = 22.dp

    // Semantic Radius Aliases
    val RadiusCard   = RadiusMedium
    val RadiusHero   = RadiusLarge
    val RadiusButton = RadiusSmall
    val RadiusBadge  = RadiusSmall
    val RadiusChip   = RadiusSmall
    val RadiusModal  = RadiusMedium
    val RadiusSheet  = RadiusExtraLarge

    // Border
    val PanelBorderWidth = 1.dp

    // Surface hierarchy
    const val BorderAlphaSubtle   = 0.08f
    const val BorderAlphaNormal   = 0.18f
    const val BorderAlphaEmphasis = 0.30f
    const val CardSurfaceAlpha    = 1.0f

    // Content alphas
    const val AlphaSheet   = 1.0f
    const val AlphaOverlay = 0.96f
    const val AlphaSecondary    = 0.70f  // Sekundärer Text und Icons
    const val AlphaDisabled     = 0.40f  // Disabled-State
    const val AlphaSubtle       = 0.08f  // Hover-Overlays, subtile Trenner

    // Spacing
    val ScreenPadding  = 16.dp
    val CardSpacing    = 12.dp
    val ContentSpacing = 10.dp
    val InnerPadding   = 16.dp
    val HeroPadding    = 18.dp

    // Elevation
    val CardElevation = 0.dp
    val HeroElevation = 0.dp
}
