# MontageZeit MCP Servers

MCP (Model Context Protocol) Server für das MontageZeit Android-Projekt. Diese Server bieten spezialisierte Tools für Code-Review, Test-Generierung und Dependency-Auditing.

## Übersicht

### Verfügbare Server

#### 1. kotlin-reviewer
Automatischer Code-Review für Kotlin-Code mit Fokus auf Android-Best Practices, Compose-Patterns und Clean Architecture.

**Warum sinnvoll:** Tägliche Code-Qualitätssicherung ohne manuelle Reviews. Erkennt Architekturverletzungen, Security-Probleme und Performance-Anti-Patterns.

**Tools:**
- `review_kotlin_file` - Review einer einzelnen Kotlin-Datei
- `review_kotlin_directory` - Review aller Kotlin-Dateien in einem Verzeichnis
- `check_compose_patterns` - Prüfung von Jetpack Compose Code
- `check_clean_architecture` - Verifizierung von Clean Architecture Prinzipien

**Beispiele:**
```bash
# Review einer Datei
node cli.js kotlin-reviewer run review_kotlin_file '{"filePath":"app/src/main/java/de/montagezeit/app/MainActivity.kt"}'

# Review eines Verzeichnisses
node cli.js kotlin-reviewer run review_kotlin_directory '{"dirPath":"app/src/main/java/de/montagezeit/app/ui/screen","recursive":true}'

# Mit Fokus auf Compose
node cli.js kotlin-reviewer run review_kotlin_file '{"filePath":"app/src/main/java/de/montagezeit/app/ui/screen/today/TodayScreen.kt","focus":"compose"}'
```

#### 2. android-tester
Generiert Unit- und Instrumented-Tests basierend auf existierendem Code. Beschleunigt Test-Coverage und reduziert manuellen Aufwand.

**Warum sinnvoll:** Automatische Test-Generierung spart Zeit und erhöht Code-Coverage. Spezialisiert auf Android-Komponenten und Clean Architecture.

**Tools:**
- `generate_unit_tests` - Generiert Unit-Tests für Kotlin-Dateien
- `generate_instrumented_tests` - Generiert instrumentierte Tests für Android-Komponenten
- `analyze_test_coverage` - Analysiert Test-Coverage und schlägt Verbesserungen vor
- `generate_usecase_tests` - Generiert Tests für UseCase-Klassen
- `generate_viewmodel_tests` - Generiert Tests für ViewModel-Klassen

**Beispiele:**
```bash
# Unit-Tests generieren
node cli.js android-tester run generate_unit_tests '{"filePath":"app/src/main/java/de/montagezeit/app/domain/usecase/RecordMorningCheckIn.kt"}'

# Instrumentierte Tests generieren
node cli.js android-tester run generate_instrumented_tests '{"filePath":"app/src/main/java/de/montagezeit/app/MainActivity.kt","componentType":"activity"}'

# Test-Coverage analysieren
node cli.js android-tester run analyze_test_coverage '{"filePath":"app/src/main/java/de/montagezeit/app/domain/location/LocationCalculator.kt"}'

# UseCase-Tests generieren
node cli.js android-tester run generate_usecase_tests '{"filePath":"app/src/main/java/de/montagezeit/app/domain/usecase/ExportDataUseCase.kt"}'
```

#### 3. gradle-auditor
Prüft Dependencies auf Sicherheitslücken, veraltete Versionen und Lizenzprobleme. Wichtig für Security-Compliance und Dependency-Management.

**Warum sinnvoll:** Automatische Security-Prüfung und Dependency-Updates verhindern Sicherheitsrisiken und halten das Projekt aktuell.

**Tools:**
- `audit_dependencies` - Audit aller Gradle-Dependencies
- `check_gradle_file` - Analyse einer spezifischen Gradle-Datei
- `check_licenses` - Prüfung von Dependency-Lizenzen
- `suggest_updates` - Schlägt Dependency-Updates vor
- `analyze_dependency_graph` - Analysiert Dependency-Graph auf Konflikte

**Beispiele:**
```bash
# Alle Dependencies auditen
node cli.js gradle-auditor run audit_dependencies '{"checkSecurity":true,"checkOutdated":true}'

# Spezifische Datei prüfen
node cli.js gradle-auditor run check_gradle_file '{"filePath":"app/build.gradle.kts"}'

# Updates vorschlagen
node cli.js gradle-auditor run suggest_updates '{"includePatch":true,"includeMinor":true,"includeMajor":false}'

# Lizenzen prüfen
node cli.js gradle-auditor run check_licenses '{"allowedLicenses":["Apache-2.0","MIT","BSD-3-Clause"]}'
```

#### 4. architecture-analyst
Analysiert die Projektstruktur auf Clean Architecture Verletzungen, Zirkelbezüge und Package-Struktur.

**Warum sinnvoll:** Verhindert Architektur-Erosion und stellt sicher, dass Layer-Regeln (z.B. Domain kennt Data nicht) eingehalten werden.

**Tools:**
- `analyze_architecture` - Analysiert Layer-Verletzungen und Zirkelbezüge
- `check_package_structure` - Validiert die Package-Hierarchie

**Beispiele:**
```bash
# Architektur prüfen
node cli.js architecture-analyst run analyze_architecture '{"sourcePath":"app/src/main/java"}'

# Struktur auflisten
node cli.js architecture-analyst run check_package_structure '{"sourcePath":"app/src/main/java"}'
```

#### 5. manifest-validator
Prüft `AndroidManifest.xml` auf Sicherheitslücken, kritische Permissions und Fehlkonfigurationen.

**Warum sinnvoll:** Verhindert Release-Blocker (z.B. debuggable=true) und Security-Risiken (z.B. ungewollt exportierte Components).

**Tools:**
- `validate_manifest` - Validiert das Manifest auf Security und Best Practices

**Beispiele:**
```bash
# Manifest validieren
node cli.js manifest-validator run validate_manifest '{"manifestPath":"app/src/main/AndroidManifest.xml"}'
```

#### 6. resource-optimizer
Findet ungenutzte Ressourcen (Strings, Farben, Drawables) und fehlende Übersetzungen.

**Warum sinnvoll:** Reduziert die App-Größe (APK) und verbessert die Lokalisierungs-Qualität.

**Tools:**
- `find_unused_resources` - Sucht nach ungenutzten Ressourcen
- `check_missing_translations` - Prüft auf fehlende Strings in Sprachdateien

**Beispiele:**
```bash
# Ungenutzte Ressourcen finden
node cli.js resource-optimizer run find_unused_resources '{"resPath":"app/src/main/res"}'

# Fehlende Übersetzungen prüfen
node cli.js resource-optimizer run check_missing_translations '{"resPath":"app/src/main/res"}'
```

## Installation

```bash
# Dependencies installieren
cd mcp_servers
npm install

# TypeScript kompilieren
npm run build

# Smoke-Tests ausführen
npm run smoke-test
```

## CLI-Nutzung

### Hilfe anzeigen
```bash
node cli.js
```

### Tools auflisten
```bash
node cli.js <server> list-tools
```

### Tool-Hilfe
```bash
node cli.js <server> help
```

### Tool ausführen
```bash
node cli.js <server> run <tool> '<args>'
```

## MCP-Integration

Die Server können über stdio mit einem MCP-Client verwendet werden:

```bash
# Server starten
node dist/index.js <server-name>
```

Beispiel-Konfiguration für MCP-Clients:

```json
{
  "mcpServers": [
    {
      "name": "kotlin-reviewer",
      "command": "node",
      "args": ["mcp_servers/dist/index.js", "kotlin-reviewer"]
    },
    {
      "name": "android-tester",
      "command": "node",
      "args": ["mcp_servers/dist/index.js", "android-tester"]
    },
    {
      "name": "gradle-auditor",
      "command": "node",
      "args": ["mcp_servers/dist/index.js", "gradle-auditor"]
    },
    {
      "name": "architecture-analyst",
      "command": "node",
      "args": ["mcp_servers/dist/index.js", "architecture-analyst"]
    },
    {
      "name": "manifest-validator",
      "command": "node",
      "args": ["mcp_servers/dist/index.js", "manifest-validator"]
    },
    {
      "name": "resource-optimizer",
      "command": "node",
      "args": ["mcp_servers/dist/index.js", "resource-optimizer"]
    }
  ]
}
```

## Entwicklung

### Neuen Server hinzufügen

1. Neue Server-Klasse in `src/servers/` erstellen
2. Von `McpServerBase` erben
3. `registerTools()` implementieren
4. `handleToolCall()` implementieren
5. In `src/index.ts` registrieren

### Tests

```bash
# Smoke-Tests
npm run smoke-test

# Linting
npm run lint

# Build
npm run build
```

## Architektur

```
mcp_servers/
├── src/
│   ├── base/
│   │   └── McpServerBase.ts    # Basisklasse für alle Server
│   ├── servers/
│   │   ├── KotlinReviewerServer.ts
│   │   ├── AndroidTesterServer.ts
│   │   └── GradleAuditorServer.ts
│   └── index.ts                # Server-Registry
├── cli.ts                      # CLI-Wrapper
├── smoke-test.js               # Smoke-Tests
├── package.json
├── tsconfig.json
└── README.md
```

## Konfiguration

Die Server unterstützen folgende Konfigurationsoptionen (über Umgebungsvariablen):

- `DEBUG` - Aktiviert Debug-Logging
- `PROJECT_ROOT` - Pfad zum Projekt-Root (Standard: automatisch erkannt)

## Best Practices

1. **Code-Review vor Commit:** Nutze `kotlin-reviewer` vor jedem Commit
2. **Test-Coverage:** Nutze `android-tester` für neue Features
3. **Security-Checks:** Nutze `gradle-auditor` regelmäßig für Dependency-Updates
4. **Smoke-Tests:** Führe `npm run smoke-test` nach Änderungen aus

## Troubleshooting

### Server startet nicht
- Prüfe ob `npm install` ausgeführt wurde
- Prüfe ob `npm run build` erfolgreich war
- Prüfe die Umgebungsvariablen

### Tools nicht gefunden
- Prüfe ob der Server korrekt registriert ist
- Prüfe `src/index.ts` auf korrekte Exporte

### Datei-Zugriff fehlgeschlagen
- Prüfe ob `PROJECT_ROOT` korrekt gesetzt ist
- Prüfe Dateiberechtigungen

## License

MIT
