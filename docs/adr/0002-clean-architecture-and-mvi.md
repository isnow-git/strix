# 0002. Clean Architecture + MVI

- Status: accepted
- Date: 2026-05-31

## Context and problem statement

We want a maintainable, testable codebase that respects SOLID, while staying
allocation-frugal on TV hardware. We need a presentation pattern that makes
state predictable for D-pad-driven Compose screens (focus, zapping, loading).

## Considered options

- Clean Architecture (domain/data/presentation) + MVI.
- Clean Architecture + MVVM.
- Pragmatic MVVM without an explicit domain layer.

## Decision

**Clean Architecture with MVI for presentation.**

- `domain` is pure Kotlin: models + **interfaces** (`ChannelRepository`,
  `StreamSource`, `CredentialReceiver`, …). It defines contracts (DIP).
- `data` implements those interfaces (Room, OkHttp, Media3).
- `presentation` (ViewModels + Compose) depends only on `domain`.
- DI wires implementations to interfaces. Interfaces stay small and focused (ISP).
- MVI: a single immutable `State`, `Intent`s in, effects out — a good fit for
  Compose recomposition and for cancellable flows (debounced zapping).

To honour performance we stay **pragmatic about model mapping**: each mapping
allocates, which is costly per list item on TV, so we avoid gratuitous layers of
DTO→domain→UI transformations where one model is sufficient.

## Consequences

- Positive: testable business logic with no Android deps; swappable data
  sources; predictable UI state.
- Positive: MVI's single state object pairs naturally with `@Immutable`/`@Stable`
  Compose state and stable list keys.
- Negative: more interfaces/boilerplate than plain MVVM. Mitigated by ISP and by
  limiting mapping layers.
