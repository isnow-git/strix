# 0010. build-logic convention plugins

- Status: accepted
- Date: 2026-05-31

## Context and problem statement

With 10 modules, per-module `build.gradle.kts` files would repeat the same
Android/Kotlin/Compose/Hilt configuration, drifting over time. We want one source
of truth for SDK levels, Java/Kotlin targets, Compose and Hilt setup.

## Considered options

- **`build-logic` included build with convention plugins** (Now-in-Android style).
- Repeating config in each module.
- `subprojects {}` / `allprojects {}` blocks in the root build (an AGP anti-pattern).

## Decision

A **`build-logic` composite build** exposing convention plugins:
`strix.android.application`, `strix.android.library`, `strix.android.compose`,
`strix.android.hilt`, `strix.kotlin.library`. SDK levels live once in
`StrixSdk` (`compile=36`, `target=36`, `min=26`). Each module's build file is
then just its plugin aliases, `namespace`, and dependencies.

## Consequences

- Positive: DRY, consistent config; a single place to bump SDK/Java/Compose;
  module builds stay tiny and readable.
- Positive: plays well with the Gradle configuration cache.
- Negative: an extra included build and the convention-plugin indirection to
  learn. Standard in modern Android and well documented.
