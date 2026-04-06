# Design Tokens (Vorschlag)

Um die Migration weg von hartcodierten Material 3 Werten zu erleichtern, definieren wir hier ein Set an neuen, exakten Design Tokens für das "Dark Control Dashboard".

Da die App aktuell stark auf MaterialTheme (`Color.kt`, `Theme.kt`, `Type.kt`) baut, sollten wir ein eigenes `CompositionLocal` (z.B. `LocalMontageZeitTokens`) einführen oder die M3-Palette strategisch "hacken" (Primary = Neon, Surface = Base Dark). Wir empfehlen Ersteres für echte Flexibilität.

## 1. Color System (Hex)

**Base & Deep Space**
*   `DeepSpaceBase`: `#0A0B0E` (Hintergrund-Solid)
*   `DeepSpaceSurface`: `#12141A` (Erhöhte Flächen ohne Glas)
*   `MeshGlowTeal`: `#003340` (Für den Ambient Background Mesh)
*   `MeshGlowAmber`: `#402000` (Für den Ambient Background Mesh)

**Glas & Alpha Surfaces (Auf Base gelegt)**
*   `GlassSurfaceLow`: `Color.White.copy(alpha = 0.03f)` (Für Standard Cards)
*   `GlassSurfaceMedium`: `Color.White.copy(alpha = 0.06f)` (Für schwebende Elemente, Listen)
*   `GlassSurfaceHigh`: `Color.White.copy(alpha = 0.12f)` (Für Bottom Sheets, Modals)

**Neon Accents & Signals**
*   `NeonPrimary` (Cyan): `#00E5FF` (Hauptaktionen, aktiver Tab)
*   `NeonPrimaryDimmed`: `#00E5FF.copy(alpha = 0.2f)` (Glow / Hover-State)
*   `StatusSuccess` (Cyber Green): `#00E676` (Abgeschlossen, Plus-Stunden)
*   `StatusWarning` (Neon Orange): `#FF9100` (Offen, Warnung)
*   `StatusError` (Alert Red): `#FF1744` (Fehler, Minus-Stunden, Löschen)
*   `StatusInfo` (Electric Blue): `#2979FF` (Neutrale Infos)

**Typography Colors**
*   `TextHigh`: `Color.White.copy(alpha = 0.95f)` (Headlines, aktive Werte)
*   `TextMedium`: `Color.White.copy(alpha = 0.65f)` (Labels, Body, Subtitles)
*   `TextLow`: `Color.White.copy(alpha = 0.35f)` (Deaktiviertes, Hilfstexte)
*   `TextOnNeon`: `#000000` (Text auf primären Buttons für maximalen Kontrast)

**Glossy Borders**
*   `BorderGlossTop`: `Color.White.copy(alpha = 0.15f)`
*   `BorderGlossBottom`: `Color.White.copy(alpha = 0.02f)`

## 2. Spacing System (dp)
Aktuell fest verdrahtet, zukünftig über ein Token-Objekt `MZSpacing`:

*   `Spacing.xxs`: `4.dp` (Inner-Element Spacing, z.B. Icon zu Text)
*   `Spacing.xs`: `8.dp` (Standard Abstand in Rows)
*   `Spacing.sm`: `12.dp` (Abstand zwischen Cards)
*   `Spacing.md`: `16.dp` (Standard Screen Padding Edge)
*   `Spacing.lg`: `24.dp` (Große Abstände, z.B. zwischen Sektionen)
*   `Spacing.xl`: `32.dp` (Hero-Padding)
*   `Spacing.xxl`: `48.dp` (Bottom Bar Clearance)

## 3. Radius System (Shape/Corner)
In `MontageZeitComponents.kt` existiert bereits `AccessibilityDefaults.CardCornerRadius`. Das wird systematisiert:

*   `Radius.sm`: `8.dp` (Badges, Tags, kleine Felder)
*   `Radius.md`: `16.dp` (Inner Cards, Standard Buttons)
*   `Radius.lg`: `24.dp` (Äußere Cards, GlassHeroCard)
*   `Radius.xl`: `32.dp` (Floating Navigation Bar, Bottom Sheets Top-Corners)
*   `Radius.pill`: `50%` (`CircleShape` für FABs, Avatare, spezielle NeonButtons)

## 4. Typography System (Fonts & Scale)
Abkehr von `SansSerif`. Definition in `Type.kt`.

*   **FontFamily.Display**: `Inter` (oder Roboto, falls keine Custom Fonts gewünscht. Inter wirkt technischer).
*   **FontFamily.Monospace**: `JetBrains Mono` oder `Roboto Mono`. Zwingend für alle Zahlen-Metriken.

**Scale (Vorschlag zur Aktualisierung in `Typography`)**
*   `DisplayLarge`: 48.sp, Bold, Monospace (Für die große Überstundenzahl im Overview).
*   `HeadlineLarge`: 32.sp, SemiBold, Display (Für Hero Titles).
*   `TitleMedium`: 18.sp, Medium, Display (Für Card Headers).
*   `LabelSmall`: 11.sp, Medium, Display, Uppercase, Tracking 0.5.sp (Für kleine Badges und Labels).
*   `BodyMedium`: 14.sp, Regular, Display, LineHeight 20.sp.

## 5. Effects (Blur & Shadows)

Da Compose Shadows in Dark Modes oft nicht sichtbar sind (weil der Hintergrund schon schwarz ist), arbeiten wir mit Drop-Glows und Blur.

*   `GlassBlurRadius`: `16.dp` bis `24.dp`. (Wird als `Modifier.blur(GlassBlurRadius)` auf Layer HINTER der transparenten Surface angewendet. Alternativ, falls Performance kritisch, nur Alpha nutzen).
*   `NeonDropGlow`: Custom Draw-Modifier, der hinter einem Button/Icon einen radialen Gradienten (`NeonPrimaryDimmed` zu Transparent) zeichnet.
