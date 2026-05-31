# 0009. Hilt for dependency injection

- Status: accepted
- Date: 2026-05-31

## Context and problem statement

Clean Architecture + DIP (ADR-0002) requires wiring implementations to interfaces.
We need DI that does **not** use runtime reflection on hot paths (zapping, scroll,
player callbacks), per the performance constraints.

## Considered options

- **Hilt** (Dagger) — compile-time codegen, zero runtime reflection.
- Manual constructor injection — zero dependency, more wiring boilerplate.
- A reflection-based DI container — **rejected outright** (runtime reflection).

## Decision

**Hilt.** It generates the graph at compile time (no runtime reflection),
integrates with Android components, ViewModels and Compose navigation, and scales
across the multi-module graph far better than hand-wiring. Manual constructor
injection is still used freely *within* modules for pure-Kotlin classes; Hilt
handles the Android-entry-point and cross-module wiring.

This was a deliberate **performance-first** call (the maintainer asked for "the
best choice for performance").

## Consequences

- Positive: no reflection in hot paths; compile-time validation of the graph;
  standard, well-documented on Android.
- Negative: KSP codegen adds build time and some annotation ceremony. Worth it
  for graph safety and zero runtime cost.
