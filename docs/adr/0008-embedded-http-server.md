# 0008. NanoHTTPD for the embedded onboarding server

- Status: accepted
- Date: 2026-05-31

## Context and problem statement

Onboarding (ADR-0005) needs a tiny HTTP server on the TV that is alive only
during the onboarding screen. We pick the implementation, weighing footprint
against the all-Kotlin/coroutines stack.

## Considered options

- **NanoHTTPD** — a single small class, blocking I/O on its own thread.
- **Ktor CIO** — coroutine-native server, fits the Kotlin stack.

## Decision

**NanoHTTPD.** The server serves one static HTML page and accepts one POST,
during onboarding only, then is stopped and GC'd. For that scope NanoHTTPD is the
lighter choice: ~one small dependency and minimal method count, versus Ktor CIO
pulling several artifacts (engine, core, io) for no runtime benefit once stopped.
On low-RAM TVs and for a lean APK, smaller dependency weight wins. Its blocking
model is a non-issue for a short-lived, single-client server, and we bridge it to
coroutines at the call site.

This was a deliberate **performance-first** call (the maintainer asked for "the
best choice for performance").

## Consequences

- Positive: minimal APK/method-count impact, trivial lifecycle, easy to audit.
- Negative: blocking thread model and a smaller feature set than Ktor; both
  irrelevant at this scope. If onboarding grows into a multi-route API, revisit
  in favour of Ktor CIO (new ADR).
