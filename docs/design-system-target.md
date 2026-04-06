# Ziel-Design-System: Dark Control Dashboard

## 1. Ziel-Stilbeschreibung & Designprinzipien
Das neue UI verabschiedet sich vom verspielten und bunten Material 3 Standard und positioniert die App als professionelles, technisches Werkzeug.

**Designprinzipien:**
*   **Deep Dark Space:** Der Hintergrund ist nicht nur grau/schwarz (`#121212`), sondern nutzt tiefdunkle Blautöne oder Obsidian mit subtilen, großflächigen farbigen "Glows" (Mesh-Gradients), um Tiefe zu erzeugen.
*   **Glassmorphism & Layering:** Karten, Panels und Bottom Sheets schweben über dem Background. Sie nutzen Transluzenz (z.B. Alpha 5% bis 15%) und Hintergrund-Unschärfe (Background Blur), um den Glow durchscheinen zu lassen.
*   **Neon & Signal:** Die Farbpalette für Aktionen, Stati und Buttons nutzt kräftige, neon-ähnliche Farben (z.B. Cyan, Cyber-Green, Alert-Red) mit starkem Kontrast auf dem dunklen Grund.
*   **Precision & Tech-Type:** Die Typografie wechselt zu klaren, serifenlosen Schriften (wie Inter) mit Monospace-Akzenten (z.B. JetBrains Mono oder Roboto Mono) für Zahlen, Zeiten und Metriken.
*   **Glossy Edges (Inner Borders):** Karten und Elemente bekommen extrem dünne, halbdurchsichtige Rahmen (`1dp`, `white.copy(alpha=0.1)` an der oberen Kante), um Lichtbrechung auf Glas zu simulieren.

## 2. Farbpalette (Target Dark)
Die Farbwelt muss von Grund auf neu gedacht werden. Keine "Day/Night" Zwangs-Invertierung mehr, sondern Dark Mode First.

*   **Deep Backgrounds (Base):** Ein extrem dunkles Anthrazit bis Fast-Black (`#0A0B0E`).
*   **Glow Accents (Mesh):** Subtile, stark geblurrte Farbflecken im Hintergrund in tiefem Petrol/Teal (`#003340`) und gedimmtem Amber/Bronze (`#402000`), die Tiefe geben.
*   **Surfaces (Glas):**
    *   Surface-Level 1 (Hintergrund-Karten): `White 5% Alpha` auf Base.
    *   Surface-Level 2 (Floating Nav, Modals): `White 10% Alpha` auf Base.
*   **Primary Action (Neon):** Ein stechendes, technisches Cyan/Teal (`#00E5FF`).
*   **Status Colors:**
    *   Success: Cyber-Green (`#00E676`)
    *   Warning: Neon-Orange (`#FF9100`)
    *   Error: Alert-Red (`#FF1744`)
*   **Text & On-Surfaces:**
    *   High Emphasis: `White 95%` (`#F2F2F2`)
    *   Medium Emphasis: `White 60%` (`#999999`)
    *   Low Emphasis: `White 30%` (`#4D4D4D`)

## 3. Typografie-System
Abkehr vom puren Android `SansSerif`.
*   **Display & Headlines:** Klar, geometrisch, leichtes Tracking (z.B. Inter SemiBold).
*   **Body:** Sehr gute Lesbarkeit, eher lockerer Zeilenabstand (z.B. Inter Regular).
*   **Metrics / Stats / Times:** Für alles, was Zahlen darstellt (wie Dashboard KPIs in `OverviewScreen`), MUSS ein **Monospace-Font** oder ein Font mit "tabular figures" (Tabellenziffern) verwendet werden (z.B. Roboto Mono). Das schafft den technischen Look.

## 4. Spacing- & Radius-System
*   **Spacing-System (Das "8pt Grid"):** Abkehr von hartcodierten Werten. Einführung von klaren Tokens:
    *   `Spacing.xs` (4.dp)
    *   `Spacing.sm` (8.dp)
    *   `Spacing.md` (16.dp)
    *   `Spacing.lg` (24.dp)
    *   `Spacing.xl` (32.dp)
*   **Radius-System (Roundness):**
    *   Outer Cards, Modals, Floating Nav: `Radius.xl` (24.dp - 28.dp). Sehr organisch, großzügig.
    *   Inner Elements (Buttons, Inner Cards): `Radius.lg` (16.dp - 18.dp).
    *   Tags/Badges: `Radius.sm` (8.dp) oder Pille (`CircleShape`).

## 5. Border- / Shadow- / Glow-System
*   **Shadows (Drop Shadow):** In Compose oft schwierig. Wenn Elevation genutzt wird, dann sehr dunkel und gestreut (z.B. Color.Black, Alpha 40%, Blur 24.dp).
*   **Borders (Glossy Edge):** Jede Glas-Komponente (`GlassCard`) bekommt einen feinen Border. Oben leicht heller (simuliert Lichteinfall), unten fast transparent.
    *   Token: `BorderStroke(1.dp, Brush.verticalGradient(listOf(Color.White.copy(0.15f), Color.White.copy(0.02f))))`.

## 6. Karten- und Buttonvarianten
*   **GlassCard:** Ersetzt `MZCard`. Nutzt Transluzenz + Background Blur + Glossy Border. Keine Material-Elevation, sondern Custom Box-Shadows wenn möglich.
*   **HighlightCard:** Ersetzt `MZHeroCard`. Bekommt einen dezenten, eingefärbten Gradient-Hintergrund (z.B. von `#00E5FF` mit 20% Alpha zu 0%), anstatt opaker Farben.
*   **NeonButton (Primary):** Ersetzt `PrimaryActionButton`. Hintergrund ist opak Primary (Cyan), Text ist Base (Schwarz), subtiler Glow-Shadow unter dem Button.
*   **GlassButton (Secondary):** Ersetzt `SecondaryActionButton` (Outlined). Hintergrund transluzent (White 5%), Border glossy, Text White 95%.
*   **GhostButton (Tertiary):** Ohne Rand, Text Primary oder White 60%.

## 7. Regeln für Dekoelemente
*   Keine massiven opaken Farbblöcke mehr.
*   Wenn eine Fläche hervorstechen soll, geschieht dies durch Helligkeit (Alpha) oder einen leuchtenden Rahmen (Glow Border), nicht durch Füllen mit einer grellen Farbe.
*   Icons sind Outline/Two-Tone (fein gezeichnet), nicht solid gefüllt, um die Leichtigkeit des Interfaces zu wahren.
