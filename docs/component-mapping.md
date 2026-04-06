# Component Mapping: Refactoring-Strategie

Diese Tabelle ordnet bestehende UI-Komponenten aus `MontageZeitComponents.kt` den Ziel-Komponenten für das Dark Control Dashboard zu und definiert den Migrationsweg.

| Bestehende Komponente | Ziel-Komponente (Neu) | Aktion | Begründung & Design-Zweck | Technische Risiken & Abhängigkeiten |
| :--- | :--- | :--- | :--- | :--- |
| `MZPageBackground` | `GlassBackground` | **Refaktorieren** | Der einfache vertikale Gradient reicht nicht. Wir brauchen Mesh-Gradients (2-3 animierte/statische, weiche Farbflecken im Hintergrund) für die Illusion von "Deep Space" und Tiefe. | Das Neu-Rendern von komplexen Gradients/Meshes kann Performance kosten. Es muss statisch genug sein, um Batterie zu schonen, aber visuell satt wirken. |
| `MZCard` | `GlassCard` | **Refaktorieren** | Standard Elevation und opake M3-Farben werden durch Transluzenz (`Color.White.copy(alpha=0.05f)`), Blur (`Modifier.blur`) und Glossy-Borders ersetzt. | Custom Shadows und Background Blur in Compose erfordern evtl. Hacks oder neuere APIs. Performance auf alten Geräten prüfen. |
| `MZStatusCard` | `GlassStatusCard` | **Ersetzen** | StatusCards füllen aktuell die ganze Karte mit einer aufdringlichen Farbe. Neu: Dunkles Glas mit starkem Neon-Rand oder leuchtendem Icon. | Aufrufstellen müssen geprüft werden, da das visuelle Gewicht extrem abnimmt. |
| `MZHeroCard` | `GlassHeroPanel` | **Refaktorieren** | Ähnlich wie `GlassCard`, aber mit einem dezenten, in die Primärfarbe getönten Mesh im Hintergrund, um als Fokuspunkt auf den Screens zu dienen (z.B. Today Status, Overview Überstunden). | - |
| `MZStatusBadge` | `NeonBadge` | **Refaktorieren** | Badges sind oft opake M3-Riegel. Ziel: Pille mit tiefschwarzem Hintergrund und hellem, farbigem Text + dünnem farbigen Glow-Border. | - |
| `PrimaryActionButton` | `NeonButton` | **Ersetzen** | Der M3 Button verliert seine standard Rounded-Corners zugunsten des neuen Token-Systems. Farbe: Stechendes Cyan/Neon. Text: Schwarz. Leichter Outer-Glow. | Das Ersetzen der Basis-M3-Komponente `Button` durch eine Custom Box/Surface erfordert Sorgfalt bei Ripple, Touch-Targets und Accessibility. |
| `SecondaryActionButton` | `GlassButton` | **Ersetzen** | Aus dem `OutlinedButton` wird ein Button mit Transluzenz und hellem Text. Outline wird ultra-dünn (Glossy Edge). | Gleiche Risiken wie bei Primary. |
| `TertiaryActionButton` | `GhostButton` | **Behalten/Tunen** | Der TextButton kann bleiben, muss aber Typografie und Padding-Tokens des neuen Systems übernehmen. | - |
| `DestructiveActionButton`| `AlertButton` | **Ersetzen** | Wie `GlassButton`, aber mit Neon-Red Akzenten statt voller roter Fläche (verhindert Augenkrebs im Dark Mode). | - |
| `MZLoadingState` | `TechLoadingState` | **Refaktorieren** | Der `CircularProgressIndicator` wirkt generisch. Ein etwas technischeres Layout (z.B. pulsierender Glow, monospace Text) wirkt hochwertiger. | - |
| `MZEmptyState` | `GlassEmptyState` | **Refaktorieren** | Icons bekommen Glow, Hintergrund wird zu Glas, Typografie wird technischer. | - |
| `MZKeyValueRow` | `TechDataRow` | **Behalten/Tunen** | Der "dotted line" Ansatz ist gut, aber Typografie (Value) MUSS monospace/tabular werden. | Tabular Figures in Compose erfordern spezifische `FontFeatureSettings` oder einen passenden Font (JetBrains Mono). |
| Standard `AlertDialog` | `GlassBottomSheet` | **Ersetzen** | M3 `AlertDialog`s (z.B. in `TodayScreen`) zerreißen das mobile Dark/Premium-Erlebnis. Ersatz durch Bottom Sheets. | `ModalBottomSheet` in Compose ist teils zickig bezüglich Keyboard-Handling und State. Hoher Umbauaufwand in den ViewModels/DialogStates. |

## Quick Wins vs. Aufwendige Umbauten

**Quick Wins (Phase 1):**
*   Anpassung der Typografie (`Type.kt`) -> Sofortiger Tech-Look.
*   Austausch der M3 Palette (`Color.kt`) -> Sofortiger Dark-Dashboard Look.
*   Umbau der `MZCard` zu `GlassCard` (nur Farbe + Border, noch kein schwerer Blur) -> Massive optische Wirkung auf allen Screens.

**Aufwendige Umbauten (Phase 2 & 3):**
*   Echter Glassmorphism (Background Blur in Compose ist komplex, wenn man Elemente hinter der Karte bewegt/scrollt).
*   Ersetzung der Dialoge durch Bottom-Sheets im `TodayScreen`.
*   Neu-Schreiben der `PrimaryActionButton` als Custom Component für perfekten Glow und Ripple.
