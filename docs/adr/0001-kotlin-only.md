# 0001. Kotlin only, no native code

- Status: accepted
- Date: 2026-05-31

## Context and problem statement

Strix runs on low-RAM Android TV hardware where smooth video and a small binary
matter. A natural question for a media app is whether parts (decoding, parsing,
networking) should drop to C/C++/Rust via the NDK for speed.

## Considered options

- **Kotlin only.**
- Kotlin + NDK (C/C++) for hot paths.
- Kotlin + Rust via JNI.

## Decision

**Kotlin only.** Video decoding goes through the hardware decoder via
`MediaCodec` (Media3/ExoPlayer orchestrates it) — language-independent, the GPU/
DSP does the work. Playlist parsing and streaming are I/O-bound, not CPU-bound,
so native code buys nothing there. A single language keeps the build simple, the
contributor pool large, and the project approachable as open source.

## Consequences

- Positive: one toolchain, no JNI boundary, smaller surface for memory bugs,
  easy CI, broad contributor reach.
- Positive: hardware decode is mandatory anyway (no software decode — see perf
  goals), and that path is native under the hood regardless of our language.
- Negative: any genuinely CPU-bound future need (e.g. custom DSP) would require
  revisiting this. Considered unlikely; would be a new ADR.
