# Genesis

Local Android music player. Compose UI, Media3 playback, Glance home-screen widget. No network.

## Features

- Browse Tracks, Albums, Artists, Folders, Audiobooks, Playlists. Sort, search, search history.
- Playlists: create, rename, delete, multi-select, reorder, dedupe on insert.
- Per-track rename via Room override; delete-from-device.
- Mark tracks as audiobooks; separate Audiobooks tab.
- Now Playing: full-screen sheet, hero artwork, Up Next queue, sleep timer (15 / 30 / 45 / 60 min), equalizer, swipe-down dismiss.
- Equalizer with custom presets via system `AudioEffect`.
- Home-screen widget: per-widget playlist or audiobooks target, transport, 4-state loop (off → repeat-all → repeat-one → shuffle), light / dark, resizable.
- Theme (light / dark / system), 8 UI locales (en, ru, es, zh, de, fr, pt, ja), 4-step font scale.
- Playback queue, shuffle, repeat survive process death.

## Stack

| Concern | Choice |
|---|---|
| Language | Kotlin 1.9.24, JVM 17 |
| UI | Jetpack Compose, Material 3, Compose BOM 2024.06 |
| Playback | AndroidX Media3 1.4.1 |
| DI | Hilt 2.51.1 (KSP) |
| Persistence | Room 2.6.1, DataStore Preferences 1.1.1 |
| Images | Coil 2.6, MaterialKolor 1.7 |
| Widget | AndroidX Glance 1.1, Glance Material 3 |
| Build | AGP 8.5.2, Gradle 8.8 |
| Min / target SDK | 30 / 35 |
| Tests | JUnit 4, Robolectric 4.14, Turbine, Mockito-Kotlin |

## Build

Requires JDK 17, Android SDK platform-35.

```bash
./gradlew assembleDebug
./gradlew installDebug
./gradlew test
./gradlew lint
```

Debug APK: `app/build/outputs/apk/debug/`.

## Release

Create `keystore/release-signing-info.txt`:

```properties
keystore_path=keystore/release.jks
alias=<key alias>
store_password=<store password>
key_password=<key password>
```

```bash
./gradlew assembleRelease
```

Without the file the APK is unsigned. R8 + resource shrinking on by default.

## Run

1. Install. Grant `READ_MEDIA_AUDIO` at the gate.
2. Library populates from `MediaStore` automatically.
3. Widget: long-press launcher → Widgets → Genesis playlist → pick target.

## Permissions

| Permission | Why |
|---|---|
| `READ_MEDIA_AUDIO` | Read tracks from `MediaStore`. |
| `POST_NOTIFICATIONS` | Playback notification on Android 13+. |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | `PlayerService`. |
| `MODIFY_AUDIO_SETTINGS` | Equalizer. |

## Module layout

`app/src/main/java/dev/maxxximgb/genesis/`

```text
├── data/
├── domain/
├── di/
├── service/
├── ui/
├── widget/
├── GenesisApp.kt
└── MainActivity.kt
```

Room schemas in `app/schemas/`.

## License

MIT — see [LICENSE](LICENSE).
