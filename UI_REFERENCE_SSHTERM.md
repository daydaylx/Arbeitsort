# UI Reference — sshterm

## 1. Kurzfazit

- **Durchgängig dunkle Oberfläche** im Default-Zustand; Light Mode existiert, ist aber sekundär (`dynamicColor = false` per Default)
- **Hero-Panel als wiederkehrendes Top-Element** auf jedem Hauptscreen — immer mit `SectionIntro` + `MetricChip`-Reihe
- **`AppScreenScaffold`** als einheitliche Screen-Hülle: zentrierte TopBar mit Titel + optionalem Subtitle, Back-Button als `FilledTonalIconButton`, transparenter Container über `AppBackdrop`
- **`AppBackdrop`** erzeugt atmosphärischen Hintergrund mit zwei dezenten Glow-Kreisen (oben links 220 dp, unten rechts 280 dp) und vertikalem Gradient
- **Panel-System auf drei Ebenen**: `AppPanel` (Standard), `AppPanel(emphasized=true)` (höherer Alpha-Wert + Shadow), `HeroPanel` (eigener Brush-Hintergrund)
- **MetricChips** sind das primäre Mittel zur schnellen Zustandsdarstellung im Hero-Bereich (3 Chips nebeneinander, gewichtet)
- **StatusChips** signalisieren farbig Zustände auf Karten- und Dialogebene (Level-Badges in Diagnostics, Session-Status im Terminal)
- **Swipe-to-Dismiss** auf HostCard: rechts = verbinden, links = löschen — mit animiertem Icon + Label im Hintergrund
- **ExpandableSection** im HostEditScreen: `ElevatedCard` mit animiertem Auf/Zu, Inhalt über `spacedBy(12.dp)` gegliedert
- **Terminal-Screen bricht aus dem Scaffold-Muster aus** — eigenes Layout mit immersivem Modus (Tap toggles UI), separates Viewport-Surface mit `RoundedCornerShape(28dp)`
- **Farben sind semantisch benannt und aliasiert**: `AppTheme.success/warning/danger/info` bilden die verbindliche Schicht über den Rohfarben
- **Monospace-Typografie nur gezielt**: `displayMedium` (Hero-Ziffern), `labelLarge/small` (Chips, Sektionsheader, Terminal-Kontext) — nie für Fließtext
- **Radien durchgängig groß**: 12 / 18 / 24 / 30 / 36 dp — erzeugt weiche, abgerundete Flächen
- **Borders sind allgegenwärtig**: Jedes Panel und jede Card hat mindestens einen 1 dp Border in `panelBorder`-Farbe
- **Tailscale-Integration** ist visuellfirst-class citizen (eigener Chip „TS", eigene Gruppe, eigene Akzentfarbe `Cyan500`)
- **String-Ressourcen** werden konsequent für alle Labels genutzt — keine Hardcoded-Texte in Composables
- **SSH-/Terminal-spezifische Elemente** (SpecialKeyBar, SessionStatusBar, TerminalViewport) sind fachlich gebunden und dürfen nicht blind übertragen werden

## 2. Visuelle Leitidee

**Stimmung:** Technisch, kontrolliert, ruhig. Die Oberfläche wirkt wie ein Instrument — keine Spielwiese, kein Social-Media-Feed. Dunkle Töne dominieren, Akzente sind sparsam und funktional gesetzt.

**Charakter:** Infrastruktur-Werkzeug mit Material-3-Grundierung. Die runden Ecken und semitransparenten Panels nehmen der technischen Domäne die Härte, ohne sie zu verharmlosen.

**Dichte:** Mittelfest. Panels haben 20 dp Innenpadding, Zwischenräume liegen bei 12–16 dp. Der Terminal-Screen ist deutlich dichter (6–10 dp), was der fachlichen Notwendigkeit geschuldet ist.

**Grad an Technikalität:** Hoch. Monospace-Labels, Fingerprint-Dialoge, Modifier-Tasten (CT/AL/SH), Session-Lifecycle-Chips — die UI kommuniziert offen, dass sie ein technisches Werkzeug ist.

**Verhältnis Ruhe / Funktionalität / Betonung:**
- Ruhe entsteht durch dunklen Hintergrund, transparente TopBar, wenige Farben außerhalb von Statuskontexten
- Funktionalität trägt die Struktur: Hero → SectionIntro → Content-Panels → Actions
- Visuelle Betonung geschieht fast ausschließlich über Farbakkente (Mint500 = Primary/Success, Cyan500 = Secondary/Info) und gelegentliche Shadow-Elevation

**Wodurch diese Wirkung erzeugt wird:**
- `AppBackdrop` mit Glow-Kreisen als subtiler Tiefeneffekt
- `Brush.verticalGradient` bzw. `Brush.linearGradient` für Hero und Background — keine Flat Surfaces, immer leichte Modulation
- Konsistente Border-Nutzung (1 dp, `panelBorder`) gibt jeder Fläche eine konturierte Kante
- Semantische Farb-Aliase (`success`, `warning`, `danger`, `info`) sorgen für Wiedererkennung über Screen-Grenzen hinweg

## 3. Screen-Architektur

### 3.1 Host List

**Struktureller Aufbau:**
```
AppScreenScaffold(title, subtitle, actions=[Settings-Button], FAB=[Add Host])
  └─ LazyColumn (verticalArrangement.spacedBy(14.dp))
       ├─ HostListHero (HeroPanel + SectionIntro + 3 MetricChips + 2 Buttons)
       ├─ [stickyHeader] SectionHeader pro HostGroup
       ├─ HostCard × N
       └─ Spacer(92.dp)
```

**Header / Top-Area:** `AppScreenScaffold` mit zentrierter TopBar — Titel „Hosts" + Subtitle, rechts Settings-Icon als `FilledTonalIconButton`.

**Hero-/Intro-Bereich:** `HeroPanel` (20 dp horizontal, 12 dp vertikal gepaddet) enthält:
- `SectionIntro`: Eyebrow („Verwaltung"), Titel („SSH Hosts"), Supporting Text
- 3 `MetricChip` nebeneinander (Gesamt, Tailscale, Key-Hosts) mit `weight(1f)`
- 2 Buttons darunter: `FilledTonalButton` (Add) + `OutlinedButton` (Settings)

**Content-Hierarchie:** Sticky Headers gruppieren nach `HostGroup` (RECENT, TAILSCALE, DIRECT, OTHER). Jede `HostCard` zeigt: Icon-Box, Name, Adresse, AuthType-Icon, „TS"-Chip bei Tailscale, Zuletzt-verbunden-Zeitstempel.

**Aktionsbereiche:** FAB (`ExtendedFloatingActionButton`) für „Add Host". Swipe rechts = verbinden, Swipe links = löschen. Default-Click = Edit.

**Status-/Metrikdarstellung:** 3 MetricChips im Hero mit semantischen Akzentfarben (`success`, `info`, `tertiary`).

**Besonderheiten:** Swipe-to-Dismiss mit animiertem Hintergrund (Scale-Animation 0.75 → 1.0). Leerer Zustand zeigt `EmptyStateView` statt Hero-Metriken.

**Wiederverwendete Muster:** HeroPanel + SectionIntro + MetricChips (Standard-Muster aller Screens). `AppScreenScaffold` als Hülle.

---

### 3.2 Host Edit

**Struktureller Aufbau:**
```
AppScreenScaffold(title, subtitle, actions=[Delete-Button wenn Edit-Modus])
  └─ Column (verticalScroll, horizontal=20dp, vertical=12dp, spacedBy=16dp)
       ├─ HeroPanel (SectionIntro)
       ├─ [optional] Busy-Panel (Working + InlineLoadingView)
       ├─ ExpandableSection „Verbindung" (Name, Host, Port, User)
       ├─ ExpandableSection „Authentifizierung" (Password/Key Chips, Key-Panel, FilePicker)
       ├─ ExpandableSection „Zieltyp" (Direct/Tailscale FilterChips)
       └─ Button-Row (Cancel + Save)
```

**Header / Top-Area:** Titel wechselt zwischen „Neuer Host" / „Host bearbeiten". Im Edit-Modus zusätzlich Delete-Button in TopBar-Actions.

**Hero-/Intro-Bereich:** `HeroPanel` mit `SectionIntro` — Eyebrow, Titel (kontextabhängig), Supporting Text. Keine MetricChips.

**Content-Hierarchie:** Drei `ExpandableSection`-Blöcke als `ElevatedCard`s mit animiertem Expand/Collapse. Default-Expansion: Verbindung = auf, Auth = auf, Zieltyp = zu.

**Aktionsbereiche:** Unten Cancel (OutlinedButton) + Save (FilledTonalButton), beide `weight(1f)`.

**Status-/Metrikdarstellung:** Keine Chips. Validierungsfehler werden inline unter den jeweiligen `OutlinedTextField`e als roter Supporting-Text angezeigt.

**Besonderheiten:**
- `FilterChip`-Paare für AuthType (Password/PrivateKey) und TargetType (Direct/Tailscale) mit Icons
- Bei gespeichertem PrivateKey: `AppPanel(emphasized=true)` mit Erfolgsmeldung in `AppTheme.success`
- FilePicker-Launcher für Key-Dateien

**Wiederverwendete Muster:** `AppScreenScaffold`, `HeroPanel`, `SectionIntro`, `AppPanel`, `SectionHeader`. Sonderfall: ExpandableSection ist screenspezifisch.

---

### 3.3 Terminal

**Struktureller Aufbau:**
```
AppBackdrop
  └─ Scaffold(containerColor=Transparent, topBar, bottomBar)
       ├─ topBar (AnimatedVisibility)
       │    └─ TerminalTopBar (AppPanel mit Status-Chips, HostName, Actions)
       ├─ bottomBar (AnimatedVisibility)
       │    └─ AppPanel(emphasized, padding=6dp)
       │         ├─ TerminalSelectionToolbar
       │         ├─ OutlinedTextField (Texteingabe)
       │         ├─ SpecialKeyBar
       │         └─ SessionStatusBar
       └─ content
            └─ Surface(RoundedCornerShape(28dp), Border)
                 └─ Box(background=terminalBackdropBrush, padding=8dp)
                      └─ TerminalViewport (LazyColumn, Monospace)
```

**Header / Top-Area:** `TerminalTopBar` als `AppPanel` (nicht `AppScreenScaffold.TopBar`). Zeigt Lifecycle-StatusChip, HostName als `headlineMedium`, unterstützenden Text, Diagnose/Disconnect/Reconnect-Buttons.

**Hero-/Intro-Bereich:** Entfällt. Der Hero-Eindruck wird durch die TopBar mit großem HostName ersetzt.

**Content-Hierarchie:** Das Viewport-Surface dominiert mit 28 dp Radien, eigenem Brush (`terminalBackdropBrush`) und 8 dp Innenpadding. Tap auf Viewport toggles `isUiVisible` → blendet TopBar und BottomBar mit Slide+Fade-Animation ein/aus.

**Aktionsbereiche:** BottomBar enthält 4 Ebenen: SelectionToolbar, Texteingabe, SpecialKeyBar, SessionStatusBar — alles in einem einzigen `AppPanel(emphasized)`.

**Status-/Metrikdarstellung:** `SessionStatusBar` als farbige Leiste unterhalb der Tastatur mit StatusChip + Beschreibungstext. `TerminalTopBar` mit Lifecycle- und Diagnose-Chips.

**Besonderheiten:**
- **Immersiver Modus**: Tap toggles gesamte UI-Chrome
- **Kein `AppScreenScaffold`** — eigenes Scaffold mit direktem `AppBackdrop`-Wrap
- **Vollständige xterm-256-Farbunterstützung** im Viewport
- **Textauswahl** per Long-Press-Drag mit Cell-Mapping
- Biometrische Authentifizierung kann vor Viewport-Anzeige geschaltet sein

**Sonderfall:** Dies ist der einzige Screen, der das `AppScreenScaffold`-Muster durchbricht. Terminal-spezifische Komponenten dominieren.

---

### 3.4 Settings

**Struktureller Aufbau:**
```
AppScreenScaffold(title, subtitle)
  └─ Column (verticalScroll, horizontal=20dp, vertical=12dp, spacedBy=16dp)
       ├─ HeroPanel (SectionIntro + 3 MetricChips)
       ├─ SettingsSectionHeader „Session"
       ├─ SettingsCard (Slider, Switches, TextField)
       ├─ SettingsSectionHeader „Terminal"
       ├─ SettingsCard (Slider × 2, Switch)
       ├─ SettingsSectionHeader „Network"
       ├─ SettingsCard (Switch)
       ├─ SettingsSectionHeader „Security"
       ├─ SettingsCard (Switch)
       ├─ SettingsSectionHeader „System"
       ├─ SettingsCard (Switch)
       ├─ SettingsSectionHeader „Diagnostics"
       └─ SettingsCard (ListItem + Button)
```

**Header / Top-Area:** Standard `AppScreenScaffold` mit Titel „Einstellungen" + Subtitle, Back-Button.

**Hero-/Intro-Bereich:** `HeroPanel` mit `SectionIntro` und 3 MetricChips (Grace Period, Font-Größe, Biometrie).

**Content-Hierarchie:** Sechs Sektionen, jede mit `SettingsSectionHeader` (UPPERCASE, sekundäre Farbe) + `SettingsCard` (`ElevatedCard`, 2 dp Shadow). Cards enthalten `SettingsSwitchItem`, `SettingsSliderItem` oder `OutlinedTextField`.

**Aktionsbereiche:** Kein FAB, keine primären Actions unten. Diagnostics-Sektion hat eigenen Button (`FilledTonalButton`) als Trailing-Content einer `ListItem`.

**Status-/Metrikdarstellung:** 3 MetricChips im Hero mit `warning`, `info`, `success` Akzenten. Slider-Werte werden in kleinen Surfaces (`secondary.copy(0.12f)`) rechts vom Slider angezeigt.

**Besonderheiten:**
- SettingsComponents sind screenspezifisch (`SettingsSwitchItem`, `SettingsSliderItem`, `SettingsCard`)
- Keine ExpandableSections — alles ist permanent sichtbar
- `ListItem` mit transparentem Container für strukturierte Einträge

**Wiederverwendete Muster:** `AppScreenScaffold`, `HeroPanel`, `SectionIntro`, `MetricChip`, `SectionHeader`. `SettingsCard` und `SettingsSwitchItem` sind settings-spezifisch.

---

### 3.5 Diagnostics

**Struktureller Aufbau:**
```
AppScreenScaffold(title, subtitle, actions=[Copy, Clear])
  └─ LazyColumn (verticalArrangement.spacedBy(14.dp))
       ├─ DiagnosticsHero (HeroPanel + SectionIntro + 2 MetricChips + Status-Panel)
       ├─ OutlinedTextField (Suchfeld)
       ├─ DiagnosticEventCard × N
       └─ Spacer(20dp)
```

**Header / Top-Area:** Standard `AppScreenScaffold`. Actions: Copy-to-Clipboard + Clear-All, beide als `FilledTonalIconButton`.

**Hero-/Intro-Bereich:** `DiagnosticsHero` = `HeroPanel` mit `SectionIntro` (Eybrow, HostName/„Diagnose", Session-Kontext), 2 MetricChips (Events, State) und optionalem `AppPanel` für letzte Statusmeldung.

**Content-Hierarchie:** Suchfeld zum Filtern (unfocused: `surface.copy(0.5f)`, focused: `surface`). `DiagnosticEventCard`s als klickbare, expandierbare Karten.

**Aktionsbereiche:** Copy All und Clear in der TopBar. Expand/Collapse pro Event-Card.

**Status-/Metrikdarstellung:** `StatusChip`-Paar pro Event (Level + Kategorie). Level-basierte Einfärbung: INFO = `info`, WARN = `warning`, ERROR = `danger` + 2 dp Border in Error-Farbe. ERROR-Cards haben `errorContainer.copy(0.4f)` als Hintergrund.

**Besonderheiten:**
- Leerer Zustand zeigt `EmptyStateView` mit BugReport-Icon
- `SelectionContainer` für Detail-Text (kopierbar)
- Clipboard-Export formatiert alle Events als strukturierten Text
- `uiState.isShowingLastSession` / `uiState.sessionId` beeinflussen Hero-Text kontextsensitiv

**Wiederverwendete Muster:** `AppScreenScaffold`, `HeroPanel`, `SectionIntro`, `MetricChip`, `StatusChip`, `EmptyStateView`. `DiagnosticEventCard` ist screenspezifisch.

## 4. Wiederverwendbare UI-Primitiven

### 4.1 AppScreenScaffold

| Aspekt | Beschreibung |
|--------|-------------|
| **Zweck** | Einheitliche Screen-Hülle mit TopBar, FAB, BottomBar, Snackbar |
| **Visuelle Rolle** | Strukturelles Grundgerüst; wrappt `AppBackdrop` + `Scaffold` |
| **Typische Inhalte** | Titel, optionaler Subtitle, Back-Button, optionale Actions, Content-PaddingValues |
| **Platzierung** | Root jedes Standardscreens (nicht Terminal) |
| **Wiederverwendungsgrad** | Hoch — 4 von 5 Hauptscreens nutzen es |
| **Verbindlich** | Titel, transparenter Container, `AppBackdrop` als Außenhülle |
| **Optional** | Subtitle, Back-Button, Actions, FAB, BottomBar, SnackbarHost |

### 4.2 AppBackdrop

| Aspekt | Beschreibung |
|--------|-------------|
| **Zweck** | Atmosphärischer Hintergrund mit Glow-Effekten und vertikalem Gradient |
| **Visuelle Rolle** | Erzeugt Tiefe und Produktcharakter; liegt hinter allen Screens |
| **Typische Inhalte** | Zwei kreisförmige Glow-Boxen + Overlay-Gradient + Content-Slot |
| **Platzierung** | Unterste Ebene, umschließt gesamten Screen-Content |
| **Wiederverwendungsgrad** | Hoch — indirekt über `AppScreenScaffold` und direkt im Terminal |
| **Verbindlich** | Background-Brush, Glow-Kreis oben links (220 dp, -56/-48 Offset), Glow-Kreis unten rechts (280 dp, +84/+96 Offset), Overlay-Gradient (Transparent → Black 0.22) |
| **Optional** | — |

### 4.3 AppPanel

| Aspekt | Beschreibung |
|--------|-------------|
| **Zweck** | Standard-Container für Content-Blöcke |
| **Visuelle Rolle** | Semitransparente Fläche mit Border, ordnet Inhalte visuell ein |
| **Typische Inhalte** | Column mit `spacedBy(12.dp)` |
| **Platzierung** | Innerhalb von Screen-Content, meist nach Hero |
| **Wiederverwendungsgrad** | Hoch — HostEdit (Busy, PrivateKey-Hinweis), Terminal (TopBar, BottomBar), Diagnostics (Status-Anzeige) |
| **Verbindlich** | `surface.copy(0.94f)`, 1 dp Border in `panelBorder`, Shape = `medium` (24 dp), ContentPadding = 20 dp |
| **Optional** | `emphasized=true` → `panelStrongColor` + 8 dp Shadow |

### 4.4 HeroPanel

| Aspekt | Beschreibung |
|--------|-------------|
| **Zweck** | Hervorgehobener Intro-Bereich pro Screen |
| **Visuelle Rolle** | visueller Anker, erzeugt Screen-Identität |
| **Typische Inhalte** | `SectionIntro` + `MetricChip`-Reihe + optionale Buttons |
| **Platzierung** | Immer am Anfang des Content-Bereichs, nach Padding |
| **Wiederverwendungsgrad** | Sehr hoch — alle 5 Hauptscreens |
| **Verbindlich** | Transparenter Container + `heroBrush` (LinearGradient), 1 dp Border, `large` Shape (30 dp), Padding horizontal 22 dp, vertikal 24 dp, `spacedBy(12.dp)` |
| **Optional** | Buttons, zusätzliche Panels innerhalb des Heros |

### 4.5 MetricChip

| Aspekt | Beschreibung |
|--------|-------------|
| **Zweck** | Kompakte Kennzahlendarstellung |
| **Visuelle Rolle** | Schnelle Zustandserfassung auf einen Blick |
| **Typische Inhalte** | Label (UPPERCASE, Monospace) + Wert (Titel-Small) |
| **Platzierung** | Immer in Reihen innerhalb von HeroPanel, `weight(1f)` |
| **Wiederverwendungsgrad** | Hoch — HostList (3×), Settings (3×), Diagnostics (2×) |
| **Verbindlich** | `accent.copy(0.12f)` als Hintergrund, 1 dp Border `accent.copy(0.2f)`, Shape = `small` (18 dp), Padding 14/10 dp |
| **Optional** | Akzentfarbe (Default = `primary`) |

### 4.6 StatusChip

| Aspekt | Beschreibung |
|--------|-------------|
| **Zweck** | Einzeiliger Status-Badge |
| **Visuelle Rolle** | Farbcodierter Zustandsindikator |
| **Typische Inhalte** | Kurzer Text (Level, Zustand, Typ) |
| **Platzierung** | In Reihen (DiagnosticEventCard, HostCard, TerminalTopBar, SessionStatusBar) |
| **Wiederverwendungsgrad** | Sehr hoch — 4 Screens + Terminal |
| **Verbindlich** | `color.copy(0.1f)` Hintergrund, 1 dp Border `color.copy(0.24f)`, Shape = `small` (18 dp), Label-Small Monospace, Padding 12/8 dp |
| **Optional** | Farbe (Default = `primary`) |

### 4.7 SectionIntro

| Aspekt | Beschreibung |
|--------|-------------|
| **Zweck** | Eyebrow + Title + Supporting Text Pattern |
| **Visuelle Rolle** | Führt einen Bereich ein, erzeugt Hierarchie |
| **Typische Inhalte** | Eyebrow (UPPERCASE, Monospace, sekundäre Farbe), Titel (Headline Medium), optionaler Supporting Text (Body Medium) |
| **Platzierung** | Immer innerhalb von HeroPanel, als erstes Kindelement |
| **Wiederverwendungsgrad** | Sehr hoch — alle 5 Hauptscreens |
| **Verbindlich** | Eyebrow = `labelLarge`, `secondary` Farbe; Titel = `headlineMedium`, `onBackground`; Supporting = `bodyMedium`, `onSurfaceVariant`; `spacedBy(6.dp)` |
| **Optional** | Supporting Text |

### 4.8 SectionHeader

| Aspekt | Beschreibung |
|--------|-------------|
| **Zweck** | Sektions-Trenner in Listen |
| **Visuelle Rolle** | Strukturelle Gliederung zwischen Content-Blöcken |
| **Typische Inhalte** | Uppercase-Label in sekundärer Farbe |
| **Platzierung** | Über Content-Gruppen (HostList Sticky Header, Settings-Sektionen) |
| **Wiederverwendungsgrad** | Mittel — HostList (sticky), Settings |
| **Verbindlich** | `labelLarge`, `secondary` Farbe, UPPERCASE |
| **Optional** | — |

### 4.9 EmptyStateView

| Aspekt | Beschreibung |
|--------|-------------|
| **Zweck** | Visueller Hinweis auf leeren Inhalt |
| **Visuelle Rolle** | Freundlicher, handlungsleitender Platzhalter |
| **Typische Inhalte** | Icon (60 dp, sekundäre Farbe) + Text (Title Medium) |
| **Platzierung** | Innerhalb von `AppPanel`, im Hauptcontent-Bereich |
| **Wiederverwendungsgrad** | Mittel — HostList, Diagnostics |
| **Verbindlich** | Wrap in `AppPanel`, Icon 60 dp, Text zentriert, `spacedBy(14.dp)` |
| **Optional** | Custom Icon (Default = AddCircle) |

### 4.10 ConfirmDialog

| Aspekt | Beschreibung |
|--------|-------------|
| **Zweck** | Bestätigung für destruktive/weightige Aktionen |
| **Visuelle Rolle** | Standard Material 3 AlertDialog mit optionalem Icon |
| **Typische Inhalte** | Icon (Warning, Tertiary-Farbe), Titel (Title Large), Nachricht (Body Medium), Confirm/Dismiss Buttons |
| **Platzierung** | Modal über allem |
| **Wiederverwendungsgrad** | Mittel — HostList (Delete), HostEdit (via Effect) |
| **Verbindlich** | `AlertDialog`, Warning-Icon in `tertiary` |
| **Optional** | Icon (kann `null` sein), Custom-Texte |

### 4.11 ExpandableSection (HostEdit-spezifisch)

| Aspekt | Beschreibung |
|--------|-------------|
| **Zweck** | Aufklappbare Formularsektionen |
| **Visuelle Rolle** | Strukturiert komplexe Formulare |
| **Typische Inhalte** | Titel (Title Medium, Bold) + Expand/Collapse-Icon + Content-Column |
| **Platzierung** | Untereinander im HostEditScreen |
| **Wiederverwendungsgrad** | Niedrig — nur HostEditScreen |
| **Verbindlich** | `ElevatedCard`, `medium` Shape, `surface` Farbe, 16 dp Padding, `spacedBy(12.dp)` im Content |
| **Optional** | Initiale Expansion (per State) |

### 4.12 Dialog-Muster (allgemein)

Alle Dialoge nutzen Material 3 `AlertDialog`. Es gibt zwei Varianten:
- **Bestätigungsdialoge** (Disconnect, Reconnect): Titel + Text + Confirm/Dismiss Buttons
- **Informationsdialoge** (Fingerprint, TailscaleHint): Titel + mehrzeiliger Content mit Monospace-Elementen + Action-Buttons

Durchgängig: `Button` für primäre Action, `OutlinedButton` für sekundäre.

## 5. Farben und Surface-System

### 5.1 Hintergrundebenen

| Ebene | Farbe / Mechanismus | Rolle |
|-------|-------------------|-------|
| **Base Background** | `AppTheme.backgroundBrush` (vertikaler Gradient: `#08131A` → `#0B1720` → `#04080C`) | Unterste Schicht, immer sichtbar durch transparente Container |
| **Glow-Overlays** | Kreis oben links (220 dp, `glowColor` = `primary.copy(0.18f)`), Kreis unten rechts (280 dp, `secondary.copy(0.08f)`) | Atmosphärische Tiefe |
| **Darkening Overlay** | Vertikaler Gradient (Transparent → `Black.copy(0.22f)`) | Abdunklung nach unten |
| **Terminal Background** | `TerminalBackground` (`#020507`) | Speziell für Terminal-Viewport |

### 5.2 Panels / Surfaces

| Surface | Farbe | Verwendung |
|---------|-------|-----------|
| `panelColor` | `surface.copy(0.94f)` | Standard `AppPanel` |
| `panelStrongColor` | `surfaceContainerHigh.copy(0.98f)` | `AppPanel(emphasized=true)` |
| `HeroPanel Background` | `heroBrush` (LinearGradient: `#123142` → `#10222E` → `#0A141B`) | Hero-Bereiche |
| `TerminalBackdrop` | `terminalBackdropBrush` (Vertikal: `#061016` → `TerminalBackground`) | Terminal-Viewport Umgebung |

### 5.3 Primäre und sekundäre Akzente (Dark Mode)

| Rolle | Farbe | Hex |
|-------|-------|-----|
| Primary | `Mint500` | `#37E3A5` |
| Secondary | `Cyan500` | `#43B9FF` |
| Tertiary | `Amber500` | `#FFC95C` |
| Error | `Rose500` | `#FF6F7D` |

### 5.4 Statusfarben (Semantische Aliase)

| Alias | Referenz | Einsatz |
|-------|----------|---------|
| `success` | `StatusConnected` = `Mint500` | Verbunden, erfolgreich, gespeicherte Keys |
| `warning` | `StatusConnecting` = `Amber500` | Verbindend, Grace Period, Warnungen |
| `danger` | `StatusError` = `Rose500` | Fehler, Löschen, ERROR-Diagnostic-Events |
| `info` | `TailscaleAccent` = `Cyan500` | Tailscale, Diagnose, neutrale Status-Infos |

### 5.5 Border-System

| Kontext | Breite | Farbe |
|---------|--------|-------|
| Standard-Panel/Card | 1 dp | `panelBorder` = `outlineVariant.copy(0.9f)` |
| ERROR-DiagnosticEventCard | 2 dp | `error.copy(0.5f)` |
| Löschen-HostCard | 2 dp | `error` |
| HeroPanel | 1 dp | `panelBorder` |

### 5.6 Glow-/Akzent-System

- **Glow-Kreise** im Backdrop nutzen stark reduzierte Alpha-Werte (0.18 / 0.08)
- **MetricChip-Hintergründe**: `accent.copy(0.12f)` — subtil, aber erkennbar
- **StatusChip-Hintergründe**: `color.copy(0.1f)` — noch dezenter
- **FilterChip-Selektion**: `primary.copy(0.18f)`
- **Slider-Werteanzeige**: `secondary.copy(0.12f)`

### 5.7 Brush-/Gradient-Nutzung

| Brush | Typ | Farben | Einsatz |
|-------|-----|--------|---------|
| `backgroundBrush` | Vertikal | `#08131A` → `#0B1720` → `#04080C` | App-Hintergrund |
| `heroBrush` | Linear | `#123142` → `#10222E` → `#0A141B` | HeroPanel |
| `terminalBackdropBrush` | Vertikal | `#061016` → `#020507` | Terminal-Viewport Umgebung |

### 5.8 Semantische vs. dekorative Farben

- **Semantisch**: `success`, `warning`, `danger`, `info` — immer an Zustände gebunden
- **Dekorativ**: Glow-Kreise, HeroBrush — erzeugen Atmosphäre ohne Informationsgehalt
- **Strukturell**: `panelBorder`, `outline`, `outlineVariant` — definieren Flächengrenzen

### 5.9 Regeln der visuellen Hierarchie

1. Primärfarbe (Mint) → Hauptaktionen, verbundene Zustände
2. Sekundärfarbe (Cyan) → Tailscale, Info-Chips, Sektionsheader
3. Tertiärfarbe (Amber) → Übergangszustände, Warnungen
4. Error-Farbe (Rose) → Fehler, destruktive Aktionen
5. `onSurfaceVariant` → Supporting Text, Labels, untergeordnete Infos

## 6. Formensprache, Spacing und Density

### 6.1 Shape-/Radius-System

Alle Shapes sind `RoundedCornerShape`:

| Token | Radius | Einsatz |
|-------|--------|---------|
| `extraSmall` | 12 dp | Kleinste Elemente (Chip-Innenflächen) |
| `small` | 18 dp | MetricChip, StatusChip, Slider-Werteanzeige |
| `medium` | 24 dp | AppPanel (Default), ExpandableSection, DiagnosticEventCard, SettingsCard, Suchfeld |
| `large` | 30 dp | HeroPanel |
| `extraLarge` | 36 dp | Größte Container |
| Terminal-Surface | 28 dp | Eigenwert (nicht im Token-System) |
| CircleShape | ∞ | Glow-Kreise im Backdrop |

### 6.2 Flächencharakter

- **Keine harten Kanten** — alles ist abgerundet
- **Borders auf jeder Panel-Fläche** — 1 dp als Standard, erzeugt „eingefasste" Optik
- **Semitransparenz** — Panels sind nie voll deckend (0.94 / 0.98), der Background schimmert durch
- **HeroPanel hat eigenen Brush** — hebt sich durch Textur, nicht nur durch Farbe ab

### 6.3 Standardabstände

| Kontext | Wert |
|---------|------|
| Screen-Content horizontal | 20 dp |
| Screen-Content vertikal (Start) | 12 dp |
| Zwischen Hauptelementen (LazyColumn) | 14–16 dp |
| Innerhalb AppPanel (ContentPadding) | 20 dp |
| Innerhalb HeroPanel | horizontal 22 dp, vertikal 24 dp |
| AppPanel interne Column spacing | 12 dp |
| SectionIntro interne Zeilen | 6 dp |
| MetricChip-Reihe | 10 dp |
| Button-Reihen | 12 dp |
| ExpandableSection intern | 12 dp |
| HostCard intern | 16 dp |
| Terminal-Surface Padding | 12 dp horizontal, 8 dp Viewport |

### 6.4 Dichte / Luftigkeit

- **Standardscreens**: mittlere Dichte. 14–16 dp zwischen Elementen, 20 dp Horizontalpadding erzeugen eine ruhige, strukturierte Anmutung
- **Terminal**: hohe Dichte. 4–6 dp zwischen Tasten, 8–10 dp Viewport-Padding — fachlich notwendig
- **Hero-Bereiche**: luftig. 24 dp vertikal, 22 dp horizontal, 12 dp zwischen den Reihen

### 6.5 Typische Größen

| Element | Größe |
|---------|-------|
| MetricChip | ~14 dp horizontal + 10 dp vertikal Padding, voller Text |
| StatusChip | ~12 dp horizontal + 8 dp vertikal Padding |
| HostCard Icon-Box | 48 dp |
| EmptyState Icon | 60 dp |
| LoadingView CircularProgressIndicator | 48 dp |
| InlineLoadingView | 20 dp |
| SpecialKey-Taste | 46 × 28 dp |
| DPad-Arrow | 32 dp |
| FAB (Extended) | Standard M3 |

### 6.6 Prominente vs. ruhige Elemente

- **Prominent**: HeroPanel (eigener Brush, groß), MetricChips (farbig), HostName im Terminal (`headlineMedium`)
- **Ruhig**: SectionHeader (sekundäre Farbe, klein), Supporting Text (onSurfaceVariant), Timestamps (Outline-Farbe)

## 7. Typografie

### 7.1 Typo-Hierarchie

| Stil | Familie | Gewicht | Größe | Linie | Einsatz |
|------|---------|---------|-------|-------|---------|
| `displayMedium` | **Monospace** | SemiBold | 34 sp | 40 sp | Hero-Ziffern (selten genutzt) |
| `headlineLarge` | SansSerif | Bold | 28 sp | 34 sp | — (reserviert) |
| `headlineMedium` | SansSerif | SemiBold | 22 sp | 28 sp | SectionIntro-Titel, Terminal HostName |
| `bodyLarge` | SansSerif | Normal | 16 sp | 24 sp | Langer Fließtext |
| `bodyMedium` | SansSerif | Normal | 14 sp | 21 sp | Supporting Text, Dialog-Nachrichten |
| `bodySmall` | SansSerif | Normal | 12 sp | 18 sp | Timestamps, Details, Diagnostic-Details |
| `titleLarge` | SansSerif | SemiBold | 20 sp | 24 sp | TopBar-Titel, Dialog-Titel |
| `titleMedium` | SansSerif | SemiBold | 17 sp | 22 sp | Card-Titel, HostCard Name |
| `titleSmall` | SansSerif | Medium | 15 sp | 20 sp | MetricChip-Wert, Settings-Titel |
| `labelLarge` | **Monospace** | Medium | 12 sp | 16 sp, **Letter-Spacing 1.1** | SectionHeader, Slider-Werte |
| `labelSmall` | **Monospace** | Medium | 11 sp | 14 sp, **Letter-Spacing 0.8** | StatusChip, SpecialKey-Labels, Timestamps |

### 7.2 Sans vs. Monospace

- **SansSerif** ist die primäre UI-Schrift — alle Titel, Labels, Body-Texte
- **Monospace** hat drei klar definierte Rollen:
  1. `displayMedium`: Hero-Ziffern (groß, präsent)
  2. `labelLarge`: Sektionsheader, Werteanzeigen (technischer Einschlag)
  3. `labelSmall`: Status-Chips, Terminal-Labels, Zeitstempel (Code-Kontext)

### 7.3 Rolle von Monospace als Produktcharakter

Monospace ist das visuelle Signal für „technischer Kontext". Es erscheint bei:
- Sektionsheadern (`SectionHeader`, `SettingsSectionHeader`) → signalisiert Struktur
- Status-Chips → signalisiert Zustand als technischen Wert
- Terminal-Viewport → fachliche Notwendigkeit
- MetricChip-Labels → verbindet Kennzahlen mit Technik-Charakter

### 7.4 Titel-, Label-, Body- und Metriksystem

- **Titel**: `titleLarge` für TopBar, `titleMedium` für Cards, `titleSmall` für Chips-Werte
- **Label**: `labelLarge` für Sektions-Trenner, `labelSmall` für Status-/Chip-Text
- **Body**: `bodyMedium` für Supporting Text, `bodySmall` für Details/Timestamps
- **Metrik**: `titleSmall` für den Wert, `labelSmall` für das Label

### 7.5 Technisch vs. neutral

- **Technisch**: Monospace-Styles, Terminal-Viewport (volle Monospace), Fingerprint-Darstellung
- **Neutral**: SansSerif-Body, Hero-SectionIntros, Settings-Beschreibungen

## 8. Interaktionsmuster

### 8.1 Navigation

- **Jetpack Compose Navigation** mit typsicheren Routes (`AppRoutes`)
- **Start Destination**: `HOST_LIST`
- **5 Routen**: HostList, HostCreate (`host_create`), HostEdit (`host_edit/{hostId}`), Terminal (`terminal/{hostId}`), Settings, Diagnostics
- **Typsichere Actions** als sealed class `NavAction`
- **Deep-Linking**: Intent mit `"session_id"` Extra → direkter Navigation zu Terminal
- **Back-Navigation**: Überall `onNavigateBack` Callback, im Scaffold als `FilledTonalIconButton`

### 8.2 Top-Bar-Muster

| Screen | Typ | Titel | Actions |
|--------|-----|-------|---------|
| HostList | `CenterAlignedTopAppBar` | Titel + Subtitle | Settings-Button |
| HostEdit | `CenterAlignedTopAppBar` | Titel + Subtitle | Delete-Button (nur Edit) |
| Settings | `CenterAlignedTopAppBar` | Titel + Subtitle | — |
| Diagnostics | `CenterAlignedTopAppBar` | Titel + Subtitle | Copy, Clear |
| Terminal | Eigenes `AppPanel` | HostName + Status | Diagnostics, Disconnect, Reconnect |

### 8.3 Actions / FAB / Button-Hierarchie

**Button-Typen nach Priorität:**
1. `FilledTonalButton` — primäre Aktionen (Save, Add Host, Connect)
2. `OutlinedButton` — sekundäre Aktionen (Cancel, Settings)
3. `FilledTonalIconButton` — kontextuelle Icon-Actions (Settings in TopBar, Delete in TopBar, Copy, Clear)
4. `TextButton` —Dialog-Buttons (Confirm, Dismiss)
5. `FilterChip` — Auswahl zwischen zwei Optionen (AuthType, TargetType)

**FAB:** Nur auf HostListScreen — `ExtendedFloatingActionButton` mit Icon + Text.

### 8.4 Expand/Collapse-Muster

- **HostEdit**: `ExpandableSection` als `ElevatedCard` mit `AnimatedVisibility(expandVertically/shrinkVertically)`
- **Diagnostics**: `DiagnosticEventCard` mit Expand/Collapse-Icon, Detail-Text in `SelectionContainer`
- **State-Management**: `rememberSaveable` für Expand-States

### 8.5 Dialogverhalten

- **Plattform**: Material 3 `AlertDialog`
- **Bestätigung**: Disconnect, Delete Host
- **Information**: Fingerprint (SSH Host Key), Tailscale-Hinweis, Reconnect
- **Eingabe**: Password-Prompt (Terminal) als `AlertDialog` mit `OutlinedTextField`
- **Dismiss**: Alle Dialoge sind dismissable; Fingerprint dismiss = Reject

### 8.6 Snackbar-Verhalten

- **Implementierung**: `SnackbarHost` + `SnackbarHostState` im Scaffold
- **Einsätze**: Toast-Nachrichten (Delete-Fehler, Save-Erfolg, Copy-Bestätigung, Connection-Errors)
- **Auslösung**: Über `UiEffect` im ViewModel → LaunchedEffect → `showSnackbar`

### 8.7 Listen- und Formularinteraktionen

**Listen:**
- `LazyColumn` mit `verticalArrangement.spacedBy(14/16.dp)`
- Sticky Headers für gruppierte Listen (HostList)
- `items(key = { it.id })` für stabile Keys

**Formulare:**
- `OutlinedTextField` mit `isError` + `supportingText` für Validierung
- `rememberSaveable` für Field-States
- `KeyboardOptions(keyboardType = Number)` für Port
- `FilterChip` für binäre Auswahl

**Swipe-to-Dismiss:**
- `SwipeToDismissBox` mit StartToEnd (Connect) und EndToStart (Delete)
- Animierter Hintergrund mit Icon + Label
- `confirmValueChange` löst Action aus, verhindert automatisches Dismiss

### 8.8 Sichtbare Statusrückmeldungen

- **MetricChips**: Quantitative Zustände (Anzahl, aktiv/deaktiv)
- **StatusChips**: Qualitative Zustände (Level, Typ)
- **SessionStatusBar**: Kontinuierliche Session-Information im Terminal
- **TerminalTopBar**: Lifecycle-Status + Diagnose-Indikator
- **InlineLoadingView**: Spinner inline bei laufenden Operationen
- **Snackbar**: Ephemere Meldungen

### 8.9 Terminal als Sonderfall

- **Immersiver Modus**: Tap auf Viewport toggles `isUiVisible`
- **TopBar + BottomBar**: Slide-In/Out mit `fadeIn/fadeOut` Animation
- **Vollbild-Viewport**: Monospace-Text mit xterm-256-Farben
- **Textauswahl**: Long-Press-Drag mit Cell-Mapping, Copy/Paste/Cancel Toolbar
- **SpecialKeyBar**: CT/AL/SH Modifier + ESC/TAB/ENT + D-Pad
- **Biometrie**: Kann vor Viewport-Anzeige geschaltet sein

## 9. SSHterm-spezifische Sondermuster

### 9.1 Klar SSH-/Terminal-spezifisch

| Element | Grund |
|---------|-------|
| `TerminalViewport` | Rendert SSH-Terminalausgabe mit xterm-256-Farben, Monospace-Font, Cell-Mapping |
| `SpecialKeyBar` | Modifier-Tasten (CT/AL/SH) und Special Keys (ESC/TAB/ENT) + D-Pad — nur für Terminal-Interaktion sinnvoll |
| `SessionStatusBar` | Zeigt SSH-Session-Lifecycle (Connected, Grace, Reconnecting, Failed) |
| `TerminalTopBar` | Session-State + Lifecycle-State + Diagnose-Info — SSH-spezifisch |
| `TerminalSelectionToolbar` | Copy/Paste für Terminal-Textauswahl |
| `FingerprintDialog` | SSH Host Key Verification — SSH-spezifisches Sicherheitskonzept |
| `DisconnectDialog` / `ReconnectDialog` | SSH-Session-Management |
| `TailscaleHintDialog` | Tailscale-Netzwerkprobleme — netzwerkspezifisch |
| `terminalBackdropBrush` | Dunkler Terminal-Hintergrund — fachlich notwendig |
| `TerminalBackground/Foreground/Cursor/Selection` | Terminal-spezifische Farben |
| Biometrie-Gate vor Terminal | SSH-Zugangsschutz — produktspezifisch |
| `TerminalSessionService` (Foreground Service) | SSH-Session im Hintergrund halten |

### 9.2 In einer Arbeitszeit-App unpassend

- **SpecialKeyBar**: Modifier-Tasten haben in einer Arbeitszeit-/Arbeitsort-App keine Entsprechung
- **SessionStatusBar**: Dauerhafte Session-Anzeige wäre dort fehl am Platz
- **Immersiver Modus**: Tap-to-hide-UI ist für eine Terminal-App sinnvoll, für eine Management-App eher nicht
- **Swipe-to-Dismiss mit Connect/Delete**: Die Doppelfunktion (rechts = Action, links = Delete) ist SSH-Host-Management geschuldet
- **FingerprintDialog**: Host-Key-Verification hat kein Äquivalent in einer Arbeitszeit-App

### 9.3 Nicht 1:1 übertragbar

- **Terminal-spezifische Farben**: `TerminalBackground`, `TerminalCursor` etc.
- **xterm-256-Farblogik**: Hardcodierte Farbzuordnungen im Viewport
- **Grace-Period-Anzeige**: SSH-Reconnect-Verhalten ist SSH-spezifisch
- **Tailscale-Metriken**: „TS"-Chip, Tailscale-Gruppe, Tailscale-Akzentfarbe

## 10. Übertragbarkeit auf andere Projekte

| Muster | Bewertung | Begründung |
|--------|-----------|------------|
| **Screen-Shell (`AppScreenScaffold`)** | ✅ Direkt übernehmbar | Generisches Scaffold mit TopBar, Backdrop, Padding |
| **Hero-Struktur (`HeroPanel` + `SectionIntro` + `MetricChip`)** | ✅ Direkt übernehmbar | Screen-übergreifendes Muster, fachlich neutral |
| **Panel-/Card-System (`AppPanel`, `HeroPanel`)** | ✅ Direkt übernehmbar | Generische Container mit Border, Alpha, Shape |
| **Chips / Statusdarstellung (`MetricChip`, `StatusChip`)** | ✅ Direkt übernehmbar | Generische Badges mit semantischen Farben |
| **Formulare (`OutlinedTextField` + Validierung)** | ✅ Direkt übernehmbar | Standard M3, screenspezifisch nur ExpandableSection |
| **Listen (`LazyColumn` + Sticky Header)** | ✅ Direkt übernehmbar | Standard M3, Gruppierung ist generisch |
| **Farbrollen (`success`, `warning`, `danger`, `info`)** | ✅ Direkt übernehmbar | Semantische Aliase sind projektunabhängig |
| **Typografie-Hierarchie** | ⚠️ Sinngemäß adaptierbar | Monospace-Anteil ist SSH-geprägt; SansSerif-Hierarchie ist übertragbar |
| **Spacing-System** | ✅ Direkt übernehmbar | 20/16/14/12 dp Raster ist generisch |
| **Buttons (FilledTonal > Outlined > Text)** | ✅ Direkt übernehmbar | Standard M3-Hierarchie |
| **Navigation (NavHost + sealed Actions)** | ✅ Direkt übernehmbar | Architekturmuster, nicht fachlich gebunden |
| **`AppBackdrop` (Glow-Kreise)** | ⚠️ Sinngemäß adaptierbar | Glow-Effekte sind Geschmackssache; Struktur (Backdrop + Content) ist übertragbar |
| **Terminal-Viewport** | ❌ Nicht übernehmen | Rein SSH-spezifisch |
| **SpecialKeyBar** | ❌ Nicht übernehmen | Rein SSH-spezifisch |
| **SessionStatusBar** | ❌ Nicht übernehmen | SSH-Session-spezifisch |
| **Immersiver Modus** | ❌ Nicht übernehmen | Terminal-spezifisches Interaktionsmuster |
| **FingerprintDialog** | ❌ Nicht übernehmen | SSH Host Key-spezifisch |
| **Swipe-to-Dismiss (Connect/Delete)** | ⚠️ Nur mit Anpassung | Swipe-Gesten sind übertragbar, aber die Semantik (Connect/Delete) ist SSH-spezifisch |
| **ExpandableSection** | ⚠️ Nur mit Anpassung | Muster ist generisch, aber Implementation (ElevatedCard + Animation) sollte am Zielprojekt ausgerichtet werden |
| **SettingsComponents** | ⚠️ Nur mit Anpassung | `SettingsSwitchItem`, `SettingsSliderItem` sind generisch, aber screenspezifisch benannt |
| **Diagnostics** | ⚠️ Nur mit Anpassung | Screen-Struktur ist übertragbar, aber DiagnosticEvent-Logik ist sshterm-spezifisch |
| **Tailscale-spezifische Elemente** | ❌ Nicht übernehmen | Netzwerk-spezifisch (TS-Chip, Tailscale-Gruppe, Tailscale-Erkennung) |

## 11. Konkrete Designregeln

1. **Jeder Hauptscreen** soll `AppScreenScaffold` als Hülle nutzen (außer fachlich zwingend anders).
2. **Jeder Hauptscreen** soll ein `HeroPanel` mit `SectionIntro` als oberstes Content-Element enthalten.
3. **Hero-Bereiche** sollen 2–3 `MetricChip`e in einer Reihe mit `weight(1f)` enthalten.
4. **MetricChip-Labels** sollen UPPERCASE und `labelSmall` (Monospace) nutzen.
5. **Statusdarstellung** soll ausschließlich über `StatusChip`e mit semantischen Farben (`success`/`warning`/`danger`/`info`) erfolgen.
6. **Es darf keine Hardcoded-Texte** in Composables geben — alle Labels über String-Ressourcen.
7. **Monospace-Typografie** darf nur für `displayMedium`, `labelLarge` und `labelSmall` verwendet werden — nicht für Body- oder Title-Texte.
8. **Kein Screen** soll mehr als eine primäre Action pro Viewport-Bereich haben.
9. **Primäraktionen** sollen als `FilledTonalButton` oder `FilledTonalIconButton` umgesetzt werden.
10. **Sekundäranzeigen** (Cancel, Abbrechen, Zurück) sollen als `OutlinedButton` umgesetzt werden.
11. **Dialog-Confirm-Buttons** sollen `Button` sein, Dismiss-Buttons `OutlinedButton` oder `TextButton`.
12. **AppPanel-ContentPadding** soll standardmäßig 20 dp betragen, es sei denn, der Inhalt erfordert Kompaktheit.
13. **Zwischen Hauptelementen** in LazyColumns soll `verticalArrangement.spacedBy(14.dp)` (kompakt) oder `16.dp` (luftig) verwendet werden.
14. **SectionHeader** sollen `labelLarge` in sekundärer Farbe und UPPERCASE sein.
15. **SectionIntro** soll immer die Reihenfolge Eyebrow → Titel → Supporting Text einhalten.
16. **Es darf keine doppelten Screen-Scaffolds** geben — `AppScreenScaffold` ist die einzige erlaubte Hülle.
17. **HeroBrush und BackgroundBrush** sollen als zentrale Tokens in `AppTheme` definiert und nicht inline erstellt werden.
18. **Borders** sollen auf allen Panel- und Card-Flächen mindestens 1 dp in `panelBorder`-Farbe betragen.
19. **ERROR-Zustände** sollen 2 dp Border in Error-Farbe und `errorContainer.copy(alpha)` als Hintergrund erhalten.
20. **Ad-hoc Karten ohne feste Rolle** sind zu vermeiden — jede Card soll einem definierten Muster (`AppPanel`, `HeroPanel`, `SettingsCard`, etc.) folgen.
21. **Swipe-to-Dismiss** soll maximal zwei Aktionen unterstützen (eine pro Richtung) mit klarer visueller Rückmeldung.
22. **Snackbar-Nachrichten** sollen über `UiEffect` im ViewModel ausgelöst werden, nicht direkt im Composable.
23. **Empty States** sollen `EmptyStateView` in einem `AppPanel` nutzen — keine leeren LazyColumn-Items ohne Hinweis.
24. **ExpandableSections** sollen initial zumindest einen Bereich aufgeklappt haben (nicht alle zu).
25. **Glow-Effekte** im Backdrop dürfen Alpha-Werte von 0.20 nicht überschreiten.
26. **MetricChip-Akzentfarben** sollen den semantischen Aliase (`success`/`warning`/`danger`/`info`) folgen — nicht Rohfarben direkt nutzen.
27. **Radien** sollen aus dem Shape-System (`extraSmall` bis `extraLarge`) kommen — keine Magic Values, außer fachlich zwingend (z. B. Terminal 28 dp).
28. **TopBar-Titel** sollen `titleLarge` SansSerif SemiBold nutzen.
29. **FilterChips** für binäre Auswahl sollen Icons als `leadingIcon` haben.
30. **Dialoge** sollen `AlertDialog` nutzen — keine Custom-Dialog-Surfaces, es sei denn, fachlich zwingend.
31. **Die Terminal-Farben** (`TerminalBackground`, `TerminalForeground`, `TerminalCursor`, `TerminalSelection`) dürfen nur im Terminal-Kontext verwendet werden.
32. **Tailscale-spezifische visuelle Elemente** (TS-Chip, Tailscale-Gruppierung) dürfen nur für Tailscale-Hosts erscheinen.
33. **Biometrische Authentifizierung** soll als vorgelagerter Gate-Mechanismus implementiert sein, nicht als inline-Dialog.
34. **StatusMessage-Texte** sollen `bodySmall` in `onSurfaceVariant` sein — niemals in Titel-Stil.
35. **Spacer unter FABs** sollen mindestens 92 dp betragen, um Überlappung zu vermeiden.

## 12. Relevante Dateien / Pfade

### Empfohlene Lesereihenfolge für einen Folge-Agent

| # | Datei | Pfad | Begründung |
|---|-------|------|------------|
| 1 | **AppTheme.kt** | `app/src/main/java/com/dlx/sshterm/ui/theme/AppTheme.kt` | Zentrale Design-Tokens: Brushes, Panel-Farben, Glow, semantische Aliase (`success`/`warning`/`danger`/`info`). Erster Anlaufpunkt für das gesamte Farbsystem. |
| 2 | **Color.kt** | `app/src/main/java/com/dlx/sshterm/ui/theme/Color.kt` | Rohfarben-Palette (Ink, Sand, Mint, Cyan, Amber, Rose, Violet) + Terminal-Spezialfarben + Status-Aliase. Grundlage aller Farbzuweisungen. |
| 3 | **Theme.kt** | `app/src/main/java/com/dlx/sshterm/ui/theme/Theme.kt` | `PrivateSSHTheme`: Dark/Light ColorScheme, Shape-Definition (12/18/24/30/36 dp), Dynamic-Color-Support, Edge-to-Edge-Konfiguration. |
| 4 | **Type.kt** | `app/src/main/java/com/dlx/sshterm/ui/theme/Type.kt` | Typografie-Hierarchie: 10 TextStyles, SansSerif vs. Monospace, Letter-Spacing, Größen. |
| 5 | **AppChrome.kt** | `app/src/main/java/com/dlx/sshterm/ui/components/AppChrome.kt` | Die wichtigsten UI-Primitiven: `AppBackdrop`, `AppScreenScaffold`, `AppPanel`, `HeroPanel`, `MetricChip`, `SectionIntro`. Das Rückgrat der Screen-Architektur. |
| 6 | **AppTopBar.kt** | `app/src/main/java/com/dlx/sshterm/ui/components/AppTopBar.kt` | Alternative TopBar-Komponente (nicht in `AppScreenScaffold` integriert). |
| 7 | **StatusChip.kt** | `app/src/main/java/com/dlx/sshterm/ui/components/StatusChip.kt` | Status-Badge mit semantischen Farben. |
| 8 | **SectionHeader.kt** | `app/src/main/java/com/dlx/sshterm/ui/components/SectionHeader.kt` | Uppercase-Sektionsheader in sekundärer Farbe. |
| 9 | **EmptyStateView.kt** | `app/src/main/java/com/dlx/sshterm/ui/components/EmptyStateView.kt` | Leer-Zustand mit Icon + Text in AppPanel. |
| 10 | **ConfirmDialog.kt** | `app/src/main/java/com/dlx/sshterm/ui/components/ConfirmDialog.kt` | Standard-Bestätigungsdialog. |
| 11 | **LoadingView.kt** | `app/src/main/java/com/dlx/sshterm/ui/components/LoadingView.kt` | Ladeanzeige (full-screen + inline). |
| 12 | **AppRoutes.kt** | `app/src/main/java/com/dlx/sshterm/navigation/AppRoutes.kt` | Route-Konstanten und Parameterrouten. |
| 13 | **AppNavHost.kt** | `app/src/main/java/com/dlx/sshterm/navigation/AppNavHost.kt` | Navigation-Setup mit allen Screen-Routen und Callbacks. |
| 14 | **NavActions.kt** | `app/src/main/java/com/dlx/sshterm/navigation/NavActions.kt` | Typsichere Navigations-Actions (sealed class). |
| 15 | **MainActivity.kt** | `app/src/main/java/com/dlx/sshterm/MainActivity.kt` | Entry Point: Edge-to-Edge, NavHost, Deep-Linking. |
| 16 | **HostListScreen.kt** | `app/src/main/java/com/dlx/sshterm/ui/hostlist/HostListScreen.kt` | Referenz-Screen für das Hero + MetricChip + LazyColumn-Muster. |
| 17 | **HostCard.kt** | `app/src/main/java/com/dlx/sshterm/ui/hostlist/HostCard.kt` | Swipe-to-Dismiss-Implementierung, Host-Darstellung. |
| 18 | **HostEditScreen.kt** | `app/src/main/java/com/dlx/sshterm/ui/hostedit/HostEditScreen.kt` | Formular mit ExpandableSections, FilterChips, Validierung. |
| 19 | **TerminalScreen.kt** | `app/src/main/java/com/dlx/sshterm/ui/terminal/TerminalScreen.kt` | Terminal-Scaffold, immersiver Modus, Dialog-Orchestrierung. |
| 20 | **TerminalViewport.kt** | `app/src/main/java/com/dlx/sshterm/ui/terminal/TerminalViewport.kt` | xterm-256-Renderer, Monospace, Selection-Handling. |
| 21 | **TerminalTopBar.kt** | `app/src/main/java/com/dlx/sshterm/ui/terminal/TerminalTopBar.kt` | Session-Status-Anzeige im Terminal. |
| 22 | **SpecialKeyBar.kt** | `app/src/main/java/com/dlx/sshterm/ui/terminal/SpecialKeyBar.kt` | SSH-Modifier-Tasten + D-Pad. |
| 23 | **SessionStatusBar.kt** | `app/src/main/java/com/dlx/sshterm/ui/terminal/SessionStatusBar.kt` | Session-Lifecycle-Anzeige. |
| 24 | **SettingsContent.kt** | `app/src/main/java/com/dlx/sshterm/ui/settings/SettingsContent.kt` | Settings-Screen mit Hero, Sektionen, Slider, Switches. |
| 25 | **SettingsComponents.kt** | `app/src/main/java/com/dlx/sshterm/ui/settings/SettingsComponents.kt` | Settings-spezifische Komponenten (SwitchItem, SliderItem, Card). |
| 26 | **DiagnosticsScreen.kt** | `app/src/main/java/com/dlx/sshterm/ui/diagnostics/DiagnosticsScreen.kt` | Diagnostic-Event-Liste, Suchfeld, Clipboard-Export. |
| 27 | **FingerprintDialog.kt** | `app/src/main/java/com/dlx/sshterm/ui/dialogs/FingerprintDialog.kt` | SSH Host Key Verification. |
| 28 | **DisconnectDialog.kt** | `app/src/main/java/com/dlx/sshterm/ui/dialogs/DisconnectDialog.kt` | SSH-Disconnect-Bestätigung. |
| 29 | **ReconnectDialog.kt** | `app/src/main/java/com/dlx/sshterm/ui/dialogs/ReconnectDialog.kt` | SSH-Reconnect-Bestätigung. |
| 30 | **TailscaleHintDialog.kt** | `app/src/main/java/com/dlx/sshterm/ui/dialogs/TailscaleHintDialog.kt` | Tailscale-Hinweisdialog. |
