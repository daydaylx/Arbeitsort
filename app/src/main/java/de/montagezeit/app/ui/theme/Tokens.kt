@file:Suppress("MagicNumber")

package de.montagezeit.app.ui.theme

import androidx.compose.ui.unit.dp

object MZTokens {
    // Radius — aligned with sshterm scale (12, 18, 24, 30, 36 dp)
    val RadiusExtraSmall = 12.dp
    val RadiusSmall      = 18.dp
    val RadiusMedium     = 24.dp
    val RadiusLarge      = 30.dp
    val RadiusExtraLarge = 36.dp

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

    // Surface hierarchy — transparent glass over solid orbs
    const val BorderAlphaSubtle   = 0.08f
    const val BorderAlphaNormal   = 0.14f
    const val BorderAlphaEmphasis = 0.22f
    const val CardSurfaceAlpha    = 0.38f
    const val GlassBorderAlpha    = 0.15f
    const val GlassBorderFadeAlpha = 0.03f
    const val GlassHighlightAlpha = 0.18f
    const val GlassInnerGlowAlpha = 0.06f

    // Content alphas
    const val AlphaGlassSheet   = 0.88f  // Bottom Sheets und Modal-Overlays
    const val AlphaGlassOverlay = 0.85f  // Screen-Hintergrund-Overlays
    const val AlphaSecondary    = 0.70f  // Sekundärer Text und Icons
    const val AlphaDisabled     = 0.40f  // Disabled-State
    const val AlphaSubtle       = 0.12f  // Hover-Overlays, subtile Trenner

    // Background Orbs — solid bodies visible through glass panels
    const val OrbAlphaPrimary       = 0.55f
    const val OrbAlphaSecondary     = 0.40f
    val OrbPrimaryRadiusDp    = 220.dp // Matched to sshterm (220 dp)
    val OrbSecondaryRadiusDp  = 280.dp // Matched to sshterm (280 dp)

    // Spacing
    val ScreenPadding  = 20.dp // Matched to sshterm (20 dp)
    val CardSpacing    = 16.dp // Between cards — wider than ContentSpacing for clearer rhythm
    val ContentSpacing = 12.dp // Within panels (vertical arrangement)
    val InnerPadding   = 20.dp // Matched to sshterm (20 dp)
    val HeroPadding    = 20.dp // HeroPanel uniform padding (was 22h/24v)

    // Elevation
    val CardElevation = 0.dp
    val HeroElevation = 0.dp
}
