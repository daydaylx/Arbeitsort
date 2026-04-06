# Management Summary: UI Redesign "Dark Control Dashboard"

## 1. Zielbild
Die MontageZeit-App verabschiedet sich vom generischen "Google Material 3"-Look und wird visuell zu einem hochwertigen, professionellen Werkzeug für Techniker und Monteure umgestaltet. Das neue "Dark Control Dashboard"-Design setzt auf:
*   **Deep Dark/Neon-Ästhetik:** Verzicht auf bunte Plastik-Flächen. Tiefdunkle Hintergründe paaren sich mit präzisen, leuchtenden Neon-Akzenten (Cyan, Cyber-Green).
*   **Glassmorphism:** Karten und Navigationselemente wirken wie leicht mattiertes Glas (Transluzenz + Blur) über einem tiefen Hintergrund, umgeben von hauchdünnen, glänzenden Rändern (Glossy Edges).
*   **Technische Präzision:** Zahlen, KPIs und Dashboards nutzen technische Monospace-Schriften, wodurch Daten sofort professioneller, exakter und "wie im Kontrollzentrum" wirken.

## 2. Größte aktuelle Probleme (Status Quo)
1.  **Mangelnde Visuelle Tiefe:** Die App nutzt opake, standardisierte Farben. Im Dark Mode wirken Statuskarten oft matschig und flach.
2.  **Fehlendes Feingefühl für Mobile UX:** Dialoge (wie Check-Ins oder Locaton-Eingaben) unterbrechen den Fluss, anstatt sich als geschmeidige Bottom-Sheets über die App zu legen.
3.  **Inkonsistente Abstände:** Spacings und Paddings sind in den Komponenten (`MontageZeitComponents.kt`) und Screens teils hardcodiert, was ein "Ein-aus-einem-Guss"-Gefühl verhindert.
4.  **Generische Typografie:** Der standard Android-Font vermittelt keine Werkzeug-Identität.

## 3. Empfohlener Weg
Das Redesign wird nicht in einem "Big Bang" durchgeführt, der die ganze App zerbricht, sondern phasenweise von den Fundamenten zu den Layouts:
1.  **Phase 1 (Tokens):** Farbpalette, Typografie und globale Abstände auf das neue System umstellen.
2.  **Phase 2 (Bausteine):** Zentrale Elemente wie `MZCard` und `PrimaryActionButton` in `GlassCard` und `NeonButton` umbauen. Dadurch profitiert die App sofort und fast automatisch überall.
3.  **Phase 3 (Layouts):** Den generischen Seiten-Background gegen einen Mesh-Glow austauschen und die Navigation anpassen.
4.  **Phase 4 (UX-Feinschliff):** Dialoge im Today-Screen zu Bottom Sheets umbauen und Hero-Dashboards (Overview) auf Monospace-Typografie umstellen.

## 4. Grober Aufwand & Komplexität
| Bereich | Maßnahme | Aufwand |
| :--- | :--- | :--- |
| **Tokens & Theme** | Neuaufbau `Color.kt`, `Theme.kt`, `Type.kt` | **Klein** (1-2 Tage) |
| **Zentrale Komponenten** | Refactoring in `MontageZeitComponents.kt` (Karten, Buttons, Badges) | **Mittel** (3-5 Tage) |
| **Screen-Layouts** | Backgrounds, Floating Nav, Status Rows in History/Overview | **Mittel** (3-4 Tage) |
| **UX-Flows reparieren** | Dialoge im TodayScreen zu Bottom-Sheets umbauen inkl. Keyboard-Handling | **Groß** (5-7 Tage) |
| **Polish** | Blur-Performance-Tuning, Glow-Animationen, Haptik | **Mittel** (2-3 Tage) |

## 5. Wichtigste Risiken
*   **Performance auf Low-End Devices:** Echter "Background Blur" (`Modifier.blur`) in Jetpack Compose kann auf älteren Android-Geräten stark ruckeln. Hier muss eine Fallback-Strategie (nur Alpha/Transluzenz ohne Unschärfe) für `SDK < 31` oder leistungsschwache Geräte eingeplant werden.
*   **Accessibility & Kontrast:** Dünne Glossy-Borders und transluzente Hintergründe neigen dazu, auf spiegelnden Displays schlecht lesbar zu sein. Der Textkontrast auf dem dunklen Base-Hintergrund muss kompromisslos hoch bleiben (`White 95%`).
*   **Zerstörung der Edit-Sheet Logik:** Der `EditEntrySheet` ist bereits hochkomplex. Umbauten an seinem Layout (Sticky Save Bar über Blur) können schnell zu Fehlern bei der Tastatur-Eingabe (ImePadding) führen.
