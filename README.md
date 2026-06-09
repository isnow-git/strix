<div align="center">

# Strix

**A fast, low-memory IPTV player for Android TV.**

[![CI](https://github.com/isnow-git/strix/actions/workflows/ci.yml/badge.svg)](https://github.com/isnow-git/strix/actions/workflows/ci.yml)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF.svg?logo=kotlin)](https://kotlinlang.org)
[![Android TV](https://img.shields.io/badge/Android%20TV-API%2026%2B-3DDC84.svg?logo=android)](https://developer.android.com/tv)

</div>

> 🦉 **Active development (v0.1.0).** The architecture, module graph and core
> features are implemented - onboarding, channel import, search, zapping,
> in-place fullscreen playback, quality grouping and an EPG timeline. The
> [backlog](https://github.com/isnow-git/strix/issues) tracks what's next; design
> decisions live in [`docs/adr/`](docs/adr/).

`Strix` (owl genus - night vision, fast, lean) is an open-source IPTV player
built specifically for **low-RAM Android TV hardware** (e.g. budget TCL sets).
Every decision is driven by two priorities: **runtime performance** (controlled
memory and network use, no jank) and a **clean, SOLID architecture**.

---

## Features

- 📺 **M3U / M3U8 playlists** and **Xtream Codes** accounts
- 🧹 **Smart channel cleanup at import** - country prefixes, codec tags and junk
  symbols stripped, so a row reads `TF1` instead of `FR - TF1 HEVC`
- 🏷️ **Quality grouping** - `BeIN Sports 1 FHD` / `HD` / `SD` fold into one
  channel with selectable variants; `+1` / replay feeds stay distinct
- 🗂️ **Accurate categories** - classified by canonical id against the
  [iptv-org](https://github.com/iptv-org/iptv) catalogue, with a keyword
  classifier as fallback (Sport, News, Cinéma, Séries, Enfants, Musique, Docs…)
- 🔍 **Instant prefix search** over thousands of channels (SQLite FTS)
- ⚡ **Smooth zapping** - debounced, cancellable channel switching plus a
  remote **number keypad** that zaps straight to a channel number
- 🖼️ **In-place fullscreen preview** with a custom 10-foot player overlay
  (auto-hide controls, zap, error + retry feedback)
- 📱 **No-cloud onboarding via QR** - the TV hosts a one-page form on the LAN;
  your phone scans a QR, fills it in, and submits. No painful remote typing,
  nothing leaves your network
- 🗓️ **EPG / program guide** timeline (XMLTV + Xtream)
- 🧭 D-pad-first **glass** UI built with **Jetpack Compose for TV**

## Tech stack

| Concern        | Choice                                            |
| -------------- | ------------------------------------------------- |
| Language       | Kotlin 2.2 (no NDK - hardware decode via MediaCodec) |
| Build          | AGP 8.13, Gradle convention plugins, version catalog |
| UI             | Jetpack Compose for TV (`tv-material`)            |
| Player         | Media3 / ExoPlayer (custom `LoadControl` + ABR)  |
| HTTP           | OkHttp (connection pooling, retry + backoff, circuit breaker) |
| Persistence    | Room over SQLite (+ FTS)                          |
| Paging         | Paging 3                                          |
| Images         | Coil 3 (downsampled to display size)             |
| Serialization  | kotlinx.serialization (streamed JSON)            |
| DI             | Hilt (codegen, zero runtime reflection)          |
| Onboarding     | NanoHTTPD (embedded LAN server), ZXing (QR)      |
| Credentials    | `androidx.security.crypto` (encrypted at rest)   |
| Min / Target   | `minSdk 26` · `compile`/`targetSdk 36` · JVM 17  |

## Architecture

Clean Architecture, **MVI** for presentation, **multi-module Gradle monorepo**.
`:core:domain` is pure Kotlin and owns the repository interfaces; `:core:data`
implements them over Room + network; feature modules depend only on `:core:model`
and `:core:domain`. Hilt wires the implementations (Dependency Inversion).

```
:app                  wiring, navigation, start-route gate, global theme

:core:model           pure-Kotlin domain models, quality parser, classifier
:core:common          StrixResult, StrixError, DispatcherProvider
:core:domain          repository + onboarding interfaces (no Android)
:core:database        Room entities, DAOs, FTS search, mappers
:core:network         OkHttp client, streaming M3U + XMLTV parsers,
                      Xtream + iptv-org clients, retry / backoff / circuit breaker
:core:data            repository impls, streaming PlaylistImporter, secure store
:core:player          Media3 wrapper, TV-tuned LoadControl + ABR config
:core:designsystem    Compose TV theme, glass surfaces, D-pad focus, image cache

:feature:channels     channel grid, search, category rail, quality select,
                      keypad zap, in-place fullscreen player + overlay
:feature:onboarding   QR + embedded LAN server, single-use TTL token
:feature:epg          program-guide timeline
```

Module configuration is centralised in [`build-logic/`](build-logic/) convention
plugins (`strix.android.application`, `strix.android.library`,
`strix.android.compose`, `strix.android.hilt`, `strix.kotlin.library`) so each
module's `build.gradle.kts` stays a few lines. SDK levels and the JVM target are
single-sourced in one extension.

## Performance principles

Streaming M3U / XMLTV parse (O(1) memory) → batched Room writes · iptv-org
catalogue decoded off the stream, never buffered whole · channel name cleanup,
quality parse and category classification run **once at import**, never on a
scroll path · flat, `@Immutable` `Channel` model with stable list keys · Paging 3
everywhere · Coil downsampled to display size with an LRU cache sized from
`ActivityManager.memoryClass` · ExoPlayer released on lifecycle stop · custom
`LoadControl` tuned for low TV RAM · OkHttp connection pooling · retry with
exponential backoff + jitter · circuit breaker for dead channels · debounced +
`collectLatest` zapping · R8 full mode in release.
See [ADR-0002](docs/adr/0002-clean-architecture-and-mvi.md).

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
