# Contributing to Strix

Thanks for your interest! Strix targets **low-RAM Android TV** hardware, so
performance and a clean architecture are first-class review criteria, not
afterthoughts.

## Ground rules

- **Language:** Kotlin only. No NDK / C++ / Rust.
- **Architecture:** Clean Architecture (domain / data / presentation), MVI for
  presentation. `domain` is pure Kotlin and owns interfaces; `data` implements
  them; `presentation` depends only on `domain`.
- **Performance is a feature.** Avoid allocations in hot paths (scroll, zapping,
  player callbacks). Never load a full playlist into memory. Justify any new
  heavy dependency.

## Workflow

1. **One issue per unit of work.** Find or open an issue first.
2. **Branch** off `main` with a short-lived feature branch:
   `feat/channels-paging`, `fix/player-leak`, `docs/adr-0007`.
3. **Commit** using [Conventional Commits](https://www.conventionalcommits.org/):
   - `feat(channels): add paged channel list`
   - `fix(player): release ExoPlayer on onStop`
   - `docs(adr): 0007 FTS vs custom search index`
   - `chore(ci): cache Gradle dependencies`
   Scopes usually match a module (`channels`, `player`, `network`, `onboarding`…).
4. **Open a small, focused PR** — one concern each — linked to its issue
   (`Closes #123`).

## Definition of Done

A change is done when it:

- compiles (`./gradlew :module:assembleDebug`),
- is unit-tested where it carries logic,
- passes static analysis (`./gradlew ktlintCheck detekt`),
- is documented (README / ADR if it introduces a decision).

## Architectural decisions

Anything structural (new module, dependency swap, cross-cutting policy) needs an
**ADR** in [`docs/adr/`](docs/adr/), [MADR](https://adr.github.io/madr/) format,
numbered sequentially. Commit it under the `docs(adr):` scope. Open a discussion
in the issue before large pivots.

## Code style

- ktlint + detekt are enforced in CI. Run them locally before pushing.
- Prefer constructor injection. Keep interfaces small and focused (ISP).
- Compose: stable/immutable state, stable list `key`s, heavy work in `remember`
  or the ViewModel — never in composition.
