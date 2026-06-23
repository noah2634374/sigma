# SpawnerUtils – Meteor Client Addon

Ein Meteor-Client-Addon für **Minecraft 1.21.11 / Meteor 26.1.2** mit vier Modulen.
Alle Module landen im Meteor-GUI unter der Kategorie **SpawnerUtils**.

## Module

### 1. Freecam Spawner Breaker (`freecam-spawner-breaker`)
Baut Spawner – auch gestackte Spawner an einer Position – ohne Sichtlinie durch
Blöcke hindurch ab. Funktioniert gut in Kombination mit Meteors Freecam.
- `range` – Reichweite ab dem echten Spielerstandort (nicht ab der Freecam-Kamera).
- `delay` – Ticks Pause zwischen den Abbau-Versuchen.
- `only-when-freecam-active` – nur abbauen, während die Freecam läuft.
- `nearest-only` – nur den nächsten Spawner treffen (ideal für gestackte Spawner).
- `swing` – Arm beim Abbauen schwingen.

Hinweis: Im Survival braucht der Abbau die normale Block-Zeit (das Modul schickt
fortlaufend Abbau-Pakete). Im Creative oder bei Instamine geht es sofort.

### 2. Spawner ESP (`spawner-esp`)
Zeichnet eine senkrechte **rote Linie**, die von jedem Spawner aufsteigt, plus
optional eine Box. So sieht man Spawner durch Wände und aus großer Entfernung.
- `chunk-range`, `trial-spawner` (auch Trial-Spawner), `line-height`,
  `line-color`, `box`, `side-color`.

### 3. Sus Chunk Finder (`sus-chunk-finder`)
Scannt geladene Chunks nach **Amethyst-Clustern**. Amethyst oberhalb der
typischen Geoden-Höhe wird als *verdächtig* (sus) eingefärbt – ein Hinweis auf
spielergebaute Farmen/Stashes statt natürlicher Generierung.
- `chunk-range`, `chunks-per-tick`, `min-y`/`max-y` (Scanbereich),
  `sus-above-y` (Schwelle für „verdächtig“), `sus-only`, `chat-notify`,
  `normal-color`, `sus-color`.

### 4. Block Entity Debug (`block-entity-debug`)
Markiert **alle Block-Entities** (Kisten, Spawner, Shulker, Öfen, Schilder …).
Block-Entities werden ohnehin client-seitig übertragen, daher sieht das Modul
auch alles **tief unter dem Deepslate-Layer** – ganz ohne extra X-Ray.
- `chunk-range`, `storage-only` (nur Behälter),
  `below-deepslate-only` + `below-y` (gezielt unter den Deepslate schauen),
  `log-on-activate`, Farben pro Typ.

## Bauen

**Empfohlen:** das offizielle, immer aktuelle Template als Basis nehmen, damit
Gradle-Wrapper und Versions-Pins garantiert stimmen:

1. Template klonen:
   `git clone --depth 1 https://github.com/MeteorDevelopment/meteor-addon-template spawnerutils`
2. Aus diesem ZIP übernehmen:
   - den kompletten Ordner `src/main/java/com/example/spawnerutils/`
   - `src/main/resources/fabric.mod.json`
   - `src/main/resources/spawnerutils.mixins.json`
   - `src/main/resources/assets/spawnerutils/icon.png`
3. Im Template die alten Beispiel-Dateien (`com/example/addon/...`) löschen.
4. `gradlew build` ausführen → fertige JAR liegt in `build/libs/`.
5. JAR zusammen mit Meteor Client in den `mods`-Ordner kopieren.

Dieses Projekt enthält auch eigene `build.gradle.kts`, `settings.gradle.kts`,
`gradle.properties` und `gradle/libs.versions.toml`. Die kannst du direkt nutzen –
falls aber eine Dependency-Version nicht auflöst, ersetze `gradle/libs.versions.toml`
durch die Datei aus dem offiziellen Template (Build-Nummern ändern sich oft).

> Es fehlen bewusst die `gradlew`/`gradlew.bat`-Wrapper und `gradle-wrapper.jar` –
> die kommen aus dem offiziellen Template (Schritt 1) und müssen nicht hier liegen.

## Hinweis
Das Addon ist nur lokal getestet im Sinne der Code-Struktur – kompiliere es vor
dem Einsatz einmal mit dem aktuellen Template, da sich Yarn-Mappings zwischen
Minecraft-Versionen ändern können (z. B. Methoden-/Feldnamen).
