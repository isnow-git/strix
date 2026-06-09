# 0012. Module structure refinement

- Status: accepted
- Date: 2026-06-09

Supersedes the concrete module list in [ADR-0003](0003-multi-module-structure.md)
(its `:core:* / :feature:*` split and dependency rules are kept).

## Context and problem statement

ADR-0003 fixed an initial module graph before the layers were implemented. While
building them out, three things proved wrong in practice:

- `:core:common` mixed pure domain models, repository interfaces and shared
  utilities in one module, so a feature that only needs a model still pulled in
  everything and the Dependency Inversion boundary was implicit, not enforced.
- `:core:ui` undersold its scope: it is a design system (theme, glass surfaces,
  D-pad focus helpers, heap-sized image loader), not a grab bag of UI.
- `:feature:player` added a separate module and a navigation seam, but playback
  is launched **in place** from the channel grid (fullscreen preview, overlay,
  keypad zap). The split bought nothing and forced shared state across a module
  boundary.

## Decision

Refine the graph while keeping ADR-0003's `:core:* / :feature:*` rules:

- **Split `:core:common`** into:
  - `:core:model` - pure-Kotlin domain models, quality parser, classifier (zero deps).
  - `:core:domain` - repository + onboarding **interfaces** only (DIP contract).
  - `:core:data` - implementations over Room + network (see [ADR-0011](0011-core-data-module.md)).
  - `:core:common` stays for genuinely cross-cutting bits (`StrixResult`,
    `StrixError`, `DispatcherProvider`).
- **Rename `:core:ui` -> `:core:designsystem`.**
- **Merge `:feature:player` into `:feature:channels`**; `:core:player` (the
  Media3 wrapper, ADR-0004) is unchanged and still consumed by the feature.

Resulting graph:

```
:app
:core:model  :core:common  :core:domain  :core:database  :core:network
:core:data   :core:player  :core:designsystem
:feature:channels  :feature:onboarding  :feature:epg
```

`:core:model` is depended on by nearly everything and depends on nothing; feature
modules depend only on `:core:model` + `:core:domain` (+ `:core:designsystem` for
UI). No module cycles.

## Consequences

- Positive: the DIP boundary is now compiler-enforced (a feature cannot see an
  implementation); `:core:model` is a tiny, dependency-free leaf.
- Positive: the module graph matches the code 1:1.
- Negative: more modules than ADR-0003 listed. Mitigated by the `build-logic`
  convention plugins (ADR-0010), which keep each `build.gradle.kts` tiny.
- Note: ADR-0011 mentions `:feature:player` looking up a channel by id; read that
  as `:feature:channels`, which now owns playback.
