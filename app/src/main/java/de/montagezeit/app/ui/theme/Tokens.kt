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

    // Border
    val PanelBorderWidth = 1.dp

    // Surface hierarchy
    const val BorderAlphaSubtle   = 0.10f
    const val BorderAlphaNormal   = 0.16f
    const val BorderAlphaEmphasis = 0.26f
    const val CardSurfaceAlpha    = 0.82f

    // Background Orbs
    const val OrbAlphaPrimary       = 0.18f // Increased to match sshterm 0.18
    const val OrbAlphaSecondary     = 0.08f // Increased to match sshterm 0.08
    val OrbPrimaryRadiusDp    = 220.dp // Matched to sshterm (220 dp)
    val OrbSecondaryRadiusDp  = 280.dp // Matched to sshterm (280 dp)

    // Spacing
    val ScreenPadding = 20.dp // Matched to sshterm (20 dp)
    val CardSpacing   = 14.dp // Matched to sshterm (14 dp)
    val InnerPadding  = 20.dp // Matched to sshterm (20 dp)

    // Elevation
    val CardElevation = 0.dp
    val HeroElevation = 0.dp
}
