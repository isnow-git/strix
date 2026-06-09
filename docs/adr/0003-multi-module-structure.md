# 0003. Multi-module Gradle structure

- Status: superseded by [ADR-0012](0012-module-structure-refinement.md)
- Date: 2026-05-31

> **Superseded.** The `:core:* / :feature:*` split and its dependency rules
> still hold; the concrete module list below was refined during implementation
> (`:core:common` split into `:core:model` / `:core:domain` / `:core:data`,
> `:core:ui` renamed to `:core:designsystem`, `:feature:player` merged into
> `:feature:channels`). See [ADR-0012](0012-module-structure-refinement.md).

## Context and problem statement

A single-module app would couple everything and rebuild the world on every
change. We want enforced layer boundaries, parallel/incremental builds, and a
structure that mirrors the architecture.

## Considered options

- Single module.
- Layered modules (`:domain`, `:data`, `:presentation`).
- **Feature + core modules** (a `:core:*` / `:feature:*` split).

## Decision

A **`:core:*` / `:feature:*` split**:

```
:app
:core:common  :core:ui  :core:database  :core:network  :core:player
:feature:channels  :feature:player  :feature:onboarding  :feature:epg
```

Dependency rules: `:feature:*` depend on `:core:*`, never on each other; `:app`
wires features together; `:core:common` is pure Kotlin with zero Android deps.
Type-safe project accessors (`projects.core.common`) are enabled.

## Consequences

- Positive: Gradle parallelises and caches per module; a change in one feature
  doesn't recompile others; boundaries are compiler-enforced.
- Positive: maps 1:1 to the architecture and to the backlog (one issue per module).
- Negative: more build files and initial setup. Mitigated by `build-logic`
  convention plugins (ADR-0010).
