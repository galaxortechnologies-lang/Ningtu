# Nigutu

**Your music, beautifully yours.** A local-first Android music player with online discovery, built with Kotlin, Jetpack Compose, Media3 and Room.

Created by [Anup Thakur](https://www.instagram.com/anup_thakurrrr/).

[![Android Build & Release](https://github.com/galaxortechnologies-lang/Ningtu/actions/workflows/android-build.yml/badge.svg)](https://github.com/galaxortechnologies-lang/Ningtu/actions/workflows/android-build.yml)

---

## Overview

Nigutu plays the music already on your phone and lets you explore an online catalog from the same app. It is a single-activity Jetpack Compose application with a real Media3 playback service, so music keeps playing when you leave the app, with notification, lock-screen and Bluetooth controls.

- **Package:** `com.nigutu.app`
- **Min SDK:** 26 (Android 8.0)
- **Target / compile SDK:** 35
- **License:** GPL-3.0

## Screenshots

Nigutu uses an original dark design system — deep black and charcoal surfaces, violet and cyan accents, rounded artwork cards and a gradient now-playing screen.

> Screenshots are produced from a device or emulator running the app. Add them under `docs/screenshots/` and embed them here once captured:
>
> ```markdown
> | Home | Player | Library |
> | --- | --- | --- |
> | ![Home](docs/screenshots/home.png) | ![Player](docs/screenshots/player.png) | ![Library](docs/screenshots/library.png) |
> ```
>
> The CI pipeline builds and tests the app but does not run an emulator, so no screenshots are committed yet.

## Features

### Playback
- AndroidX Media3 / ExoPlayer engine inside a `MediaSessionService`
- Background playback with a media notification, lock-screen controls and Bluetooth/headset buttons
- Audio focus handling, headphone-unplug pause, queue management
- Shuffle, repeat (off/all/one), seek, playback speed, sleep timer with live countdown
- Mini player above the bottom navigation, full-screen player and queue screen

### Local library
- MediaStore scanning with scoped-storage friendly permissions (`READ_MEDIA_AUDIO`, legacy read for Android 8–12)
- Track, album and artist metadata with album artwork
- Favourites, playlists, recently played and listening history persisted in Room
- Sorting and filtering across favourites, playlists, albums, artists and local tracks

### Online discovery
- Public Deezer catalog: global charts, per-genre charts and search (no API key)
- Playable 30-second preview streams — Nigutu does not circumvent DRM or access controls
- Robust HTTP layer with timeouts, retry, loading, empty and error states
- "Stream over Wi-Fi only" safeguard that skips online tracks on mobile data

### Lyrics and extras
- LRCLIB lyrics with time-synchronised LRC parsing and auto-scrolling
- Platform equaliser and bass boost when the device exposes them
- Playlist JSON export/import, share playlist

### Not implemented (documented as pending)
- Home-screen widgets, Android Auto browsing and Chromecast
- Full-length streaming for catalog tracks (requires a licensed provider)
- Per-quality audio selection (the preview provider offers a single quality)
- Offline downloads of online tracks

## Technology stack

| Area | Choice |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose, Material 3, custom design system |
| Architecture | Clean architecture + MVVM, repositories and use cases |
| DI | Hilt |
| Async | Coroutines and Flow |
| Persistence | Room (metadata) and DataStore (preferences) |
| Media | AndroidX Media3 ExoPlayer, MediaSessionService |
| Networking | OkHttp + kotlinx.serialization |
| Images | Coil |
| Build | Gradle Kotlin DSL, AGP 8.5, KSP |
| CI/CD | GitHub Actions → debug APK → GitHub prerelease |

## Project architecture

```
app/src/main/java/com/nigutu/app/
├── core/
│   ├── di/              Hilt modules (database, network, repositories)
│   └── util/            LRC parser, formatters, connectivity
├── data/
│   ├── local/           Room entities, DAOs, database, mappers
│   ├── mediastore/      MediaStore scanner behind LocalMusicSource
│   ├── remote/          Deezer catalog, LRCLIB lyrics, HTTP client
│   └── repository/      Repository implementations
├── designsystem/        Theme, colours, typography, shared components
├── domain/
│   ├── model/           Song, Album, Artist, Playlist, Lyrics, settings
│   ├── repository/      Repository contracts
│   └── usecase/         Toggle favourite, refresh library, lyrics, playlists
├── feature/
│   ├── home/ explore/ search/       Discovery surfaces
│   ├── library/                     Favourites, playlists, albums, artists
│   ├── player/ lyrics/              Playback and lyrics
│   └── settings/                    Theme, playback, equaliser, about
├── navigation/          Routes and NavHost
├── playback/            MediaSessionService, controller, audio effects
└── ui/                  App shell, Netflix-style chrome, permission flow
```

Data flows one way: Compose screens observe ViewModels, ViewModels call use cases and repositories, repositories own Room and network access, and playback is reached only through `PlaybackConnection` (a media controller, never a second player).

## Build instructions

Requirements: JDK 17 and the Android SDK with platform 35 and build-tools 35.0.0.

```bash
# 1. Point Gradle at your SDK (or set ANDROID_HOME)
echo "sdk.dir=$HOME/Android/Sdk" > local.properties

# 2. Install SDK packages if needed
sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"

# 3. Run unit tests
./gradlew testDebugUnitTest

# 4. Build the debug APK
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# 5. Install on a connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Release builds are minified with R8 and intentionally unsigned in this repository; add your own signing config before distributing.

## Development instructions

- `./gradlew projects` – verify the module graph
- `./gradlew testDebugUnitTest` – JVM unit tests (LRC parser, formatters, playlist use case, repository)
- `./gradlew assembleDebug` – full compile, KSP (Room + Hilt) and packaging
- Instrumentation and Compose UI tests require a device or emulator: `./gradlew connectedDebugAndroidTest`

### Adding an online provider

1. Implement `CatalogRepository` (or a new provider interface) in `data/remote/`.
2. Map the provider payload into `Song` with a namespaced id, e.g. `remote:provider:123`.
3. Bind the implementation in `core/di/AppModule.kt`.
4. Keep credentials out of source control; see `.env.example`.

### Privacy and security

- No credentials are stored in the repository.
- Only audio-related permissions are requested; nothing is uploaded.
- Lyrics and charts are fetched over HTTPS from public, keyless APIs.
- Room is the only local store and keeps metadata only, never audio.

## GitHub Actions

`.github/workflows/android-build.yml` runs on pushes to `main`, pull requests and manual dispatch:

1. Checkout, JDK 17, Android SDK 35 and build-tools 35.0.0
2. Gradle caching, configuration check
3. Unit tests
4. Debug APK build
5. APK existence, size and package-name verification (`com.nigutu.app`)
6. APK uploaded as the `Nigutu-debug-apk` artifact
7. On `main`, a prerelease tagged `v1.0.0-debug.<run>.<attempt>` with `Nigutu-debug.apk`

The release step needs **Settings → Actions → General → Workflow permissions → Read and write permissions** (or the `contents: write` permission the workflow already declares).

## APK download

- **Releases:** <https://github.com/galaxortechnologies-lang/Ningtu/releases>
- **Latest prerelease:** <https://github.com/galaxortechnologies-lang/Ningtu/releases/latest>
- **Actions artifacts:** <https://github.com/galaxortechnologies-lang/Ningtu/actions>

Download `Nigutu-debug.apk`, allow installation from unknown sources, install and grant audio access.

## License

Nigutu is free software released under the **GNU General Public License v3.0**. See [LICENSE](LICENSE).

## Credits

- Creator and designer: **Anup Thakur** — [@anup_thakurrrr](https://www.instagram.com/anup_thakurrrr/)
- Built with AndroidX Media3, Jetpack Compose, Room, Hilt, Coil, OkHttp
- Catalog data: [Deezer public API](https://developers.deezer.com/api) · Lyrics: [LRCLIB](https://lrclib.net)
