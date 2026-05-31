# 0004. Media3/ExoPlayer over a custom player

- Status: accepted
- Date: 2026-05-31

## Context and problem statement

The player is the heart of an IPTV app: MediaCodec orchestration, adaptive HLS,
A/V sync, subtitles, DRM hooks. We must decide build-vs-buy for the player core
while still meeting strict memory/buffering goals on low-RAM TVs.

## Considered options

- **Media3/ExoPlayer**, customising only the policy layer.
- A custom player on top of raw `MediaCodec` + `MediaExtractor`.

## Decision

**Use Media3/ExoPlayer.** Reimplementing A/V sync, adaptive streaming and codec
edge-cases would be strictly worse and a maintenance sink. Instead we customise
the parts where custom genuinely wins, **inside** Media3:

- a custom `LoadControl` (buffer min/max sized for TV RAM),
- a calibrated ABR / track-selection policy,
- lifecycle-bound release (free the player on `onStop`/`onDispose` to avoid
  zapping leaks).

This frontier — buy the engine, build the policy — is the same logic applied to
OkHttp (HTTP/TLS) and Room/SQLite.

## Consequences

- Positive: battle-tested playback; we spend effort only on the TV-specific
  tuning that matters.
- Positive: Media3 is actively maintained and Compose-friendly.
- Negative: must keep ProGuard/R8 keeps for ExoPlayer's reflective paths (done
  in `app/proguard-rules.pro`).
