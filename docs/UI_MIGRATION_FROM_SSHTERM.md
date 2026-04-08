# UI Migration From sshterm

## Übernommene Prinzipien

- Gemeinsame Screen-Shell mit ruhigem Top-Chrome und konsistenter Bottom-Navigation für Today, Übersicht und Verlauf.
- Wiederkehrende Hero-Struktur mit `MZHeroPanel` und `MZSectionIntro` als erster Block pro Hauptscreen.
- Einheitliches Panel-System über `MZAppPanel` beziehungsweise `MZContentCard` statt screen-spezifischer Card-Varianten.
- Semantische Status- und Metrikdarstellung über `MZMetricChip`, `MZStatusChip`, `MZStatusBadge` und `MZInlineNotice`.
- Konsequente Spacing-, Border- und Radius-Logik aus den bestehenden `MZTokens`.
- Klare Action-Hierarchie mit `PrimaryActionButton`, `SecondaryActionButton` und `TertiaryActionButton`.
- Ruhigere Dialog- und Sheet-Grammatik mit derselben Panel- und Action-Sprache wie die Hauptscreens.

## Bewusst nicht übernommen

- Terminal-, SSH- oder Host-spezifische UI-Muster aus sshterm.
- Immersive Speziallayouts, Session-/Terminal-Overlays und terminalbezogene Toolbars.
- Monospace-lastige Terminal-Anmutung als globales Stilmittel.
- sshterm-spezifische Spezialkarten oder Infrastruktur-Metaphern, die fachlich nicht zu Arbeitsort passen.

## Ersetzte alte Arbeitsort-Strukturen

- Die alte Home-`Scaffold`-Lösung in `MontageZeitNavGraph` wurde durch eine gemeinsame `MZHomeShellScaffold` ersetzt.
- Today, Übersicht und Verlauf nutzen nicht mehr jeweils eine eigene konkurrierende TopBar-/Scaffold-Struktur.
- History erhielt eine Hero-Zone und ein konsistentes Filter-/Header-System im selben Panel-Rahmen wie Today und Übersicht.
- Settings nutzt dieselbe Hero-, Section- und Metric-Grammatik wie die übrigen Screens.
- Edit-Sheet, Validierungshinweise und Export-Vorschau wurden an dieselbe Panel- und Action-Sprache angebunden.

## Entfernte Alt-Komponenten

- Die ungenutzte alternative Card-/Hero-Familie in `MZLayoutComponents.kt` wurde entfernt.
- Doppelte `MZSectionHeader`- und `MZEmptyState`-Varianten wurden auf eine kanonische Implementierung reduziert.
- Alte AssistChip-basierte History-Sonderaktionen wurden durch das globale Action-System ersetzt.

## Vollständig migrierte Screens

- Today
- Übersicht
- Verlauf
- Einstellungen
- Edit Entry Sheet
- Export Preview Sheet

## Restbaustellen

- Eine visuelle Endabnahme auf Gerät oder Emulator steht noch aus.
- Weitere Feinschliffe an kleineren Helper-/Picker-Komponenten können folgen, die Basisstruktur ist aber vereinheitlicht.
- Nach einer breiteren Test-/Lint-Runde können noch ungenutzte Imports oder kleinere Stilreste bereinigt werden.
