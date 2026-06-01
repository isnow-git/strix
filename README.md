<div align="center">

# Strix

**A fast, low-memory IPTV player for Android TV.**

[![CI](https://github.com/isnow-git/strix/actions/workflows/ci.yml/badge.svg)](https://github.com/isnow-git/strix/actions/workflows/ci.yml)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF.svg?logo=kotlin)](https://kotlinlang.org)
[![Android TV](https://img.shields.io/badge/Android%20TV-API%2026%2B-3DDC84.svg?logo=android)](https://developer.android.com/tv)

</div>

> ⚠️ **Early development.** The repository skeleton, architecture, and module
> graph are in place; features are being built module by module. See the
> [backlog](https://github.com/isnow-git/strix/issues) and [`docs/adr/`](docs/adr/).

`Strix` (owl genus — night vision, fast, lean) is an open-source IPTV player
built specifically for **low-RAM Android TV hardware** (e.g. budget TCL sets).
Every decision is driven by two priorities: **runtime performance** (controlled
memory and network use, no jank) and a **clean, SOLID architecture**.

---

## Features

- 📺 M3U playlists **and** Xtream Codes accounts
- 🔍 Instant prefix search over thousands of channels (SQLite FTS)
- ⚡ Smooth zapping with debounced, cancellable channel switching
- 🪟 Modern **liquid-glass** channel home (Outfit type scale, D-pad focus ring)
- 👁️ **Live side preview**: the focused channel streams muted-then-audible in a
  panel with logo, now/next EPG and synopsis; pressing OK zooms the *same*
  stream to fullscreen with no re-buffer
- 🗂️ **Browse by category** — canonical rail backed by iptv-org + a keyword
  classifier fallback
- 🎚️ **Quality grouping** — one row per channel, automatic fallback across SD/HD/4K variants
- 🗓️ **EPG now/next** — XMLTV ingestion + Xtream provider fallback, timeshift-corrected
- 📱 **No-cloud onboarding via QR**: the TV hosts a one-page form on the LAN; your
  phone scans a QR, fills it in, and submits — no painful remote typing
- 🧭 D-pad-first UI built with **Jetpack Compose for TV**

## Tech stack

| Concern        | Choice                                        |
| -------------- | --------------------------------------------- |
| Language       | Kotlin (no NDK — hardware decode via MediaCodec) |
| UI             | Jetpack Compose for TV                         |
| Player         | Media3 / ExoPlayer (custom `LoadControl` + ABR) |
| HTTP           | OkHttp (connection pooling, retry + backoff)   |
| Persistence    | Room over SQLite (+ FTS)                        |
| Paging         | Paging 3                                        |
| Images         | Coil (downsampled to display size)             |
| DI             | Hilt (codegen, zero runtime reflection)        |
| Onboarding     | NanoHTTPD (embedded), ZXing (QR)               |
| Min / Target   | `minSdk 26` · `compileSdk`/`targetSdk 36`      |

## Architecture

Clean Architecture in three layers, **MVI** for presentation, **multi-module
Gradle monorepo**. `domain` is pure Kotlin and owns the interfaces; `data`
implements them; `presentation` depends only on `domain`. DI wires the
implementations (Dependency Inversion).

```
:app                  wiring, navigation, global theme
:core:common          domain models, Result, utils (pure Kotlin, no Android)
:core:ui              Compose TV theme, D-pad focus helpers
:core:database        Room, DAOs, FTS
:core:network         OkHttp, custom streaming M3U/EPG parser
:core:player          Media3 wrapper (custom LoadControl + ABR)
:feature:channels     channel list + search
:feature:player       fullscreen playback
:feature:onboarding   QR + embedded LAN server
:feature:epg          program guide (optional)
```

Module configuration is centralised in [`build-logic/`](build-logic/) convention
plugins (`strix.android.library`, `strix.android.compose`, `strix.android.hilt`,
`strix.kotlin.library`) so each module's `build.gradle.kts` stays a few lines.

## Performance principles

Streaming M3U parse (O(1) memory) → batched Room writes · Paging 3 everywhere ·
LRU cache sized from `ActivityManager.memoryClass` · ExoPlayer released on
lifecycle stop · custom `LoadControl` tuned for low TV RAM · OkHttp connection
pooling · retry with exponential backoff + jitter · circuit breaker for dead
channels · debounced + `collectLatest` zapping · `@Immutable`/`@Stable` Compose
state with stable list keys · R8 full mode in release · allocation-frugal hot
paths. See [ADR-0002](docs/adr/0002-clean-architecture-and-mvi.md).

## Build & run

Requirements: JDK 17+, Android SDK with `platform 36` and a recent build-tools.

```bash
# Build the debug APK
./gradlew :app:assembleDebug

# Install to a connected Android TV / emulator
./gradlew :app:installDebug

# Unit tests + static analysis
./gradlew test ktlintCheck detekt
```

### Testing on a real TV over ADB Wi-Fi

```bash
# (one-time, with the TV on USB or via its on-screen pairing)
adb tcpip 5555
adb connect <tv-ip>:5555
./gradlew :app:installDebug
```

### Emulator

Use an **Android TV (1080p)** AVD on API 30+ to exercise the D-pad focus model.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). We use Conventional Commits, trunk-based
development with short-lived feature branches, and one issue per unit of work.
Architectural decisions are recorded as ADRs in [`docs/adr/`](docs/adr/).

## License

[Apache License 2.0](LICENSE).
