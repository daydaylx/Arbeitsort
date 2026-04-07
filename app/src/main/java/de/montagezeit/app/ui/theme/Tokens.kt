package de.montagezeit.app.ui.theme

import androidx.compose.ui.unit.dp

object MZTokens {
    // Radius — aligned with Shape.kt scale
    val RadiusCard   = 24.dp   // Shape.large
    val RadiusHero   = 32.dp   // Shape.extraLarge
    val RadiusButton = 50.dp   // Pill
    val RadiusBadge  = 50.dp   // Pill
    val RadiusChip   = 16.dp   // Shape.medium
    val RadiusModal  = 32.dp

    // Glass — Border-Transparenzen (semantic hierarchy)
    val BorderAlphaSubtle   = 0.12f  // Divider, passive Elemente
    val BorderAlphaNormal   = 0.20f  // Karten, NavBar
    val BorderAlphaEmphasis = 0.40f  // Hero-Karte, aktive Elemente
    val CardSurfaceAlpha    = 0.12f  // Karten-Hintergrund-Alpha

    // Background Orbs
    val OrbAlphaPrimary       = 0.14f
    val OrbAlphaSecondary     = 0.07f
    val OrbPrimaryXFraction   = 0.15f
    val OrbPrimaryYFraction   = 0.12f
    val OrbSecondaryXFraction = 0.88f
    val OrbSecondaryYFraction = 0.82f
    val OrbPrimaryRadiusDp    = 320.dp
    val OrbSecondaryRadiusDp  = 220.dp

    // Spacing
    val ScreenPadding = 16.dp
    val CardSpacing   = 12.dp
    val InnerPadding  = 16.dp

    // Compact density (touch-safe)
    val PrimaryButtonHeight = 52.dp
    val SecondaryButtonHeight = 52.dp
    val TertiaryButtonHeight = 48.dp
    val ChipHeight = 40.dp

    // Elevation
    val CardElevation = 0.dp
    val HeroElevation = 2.dp
}
