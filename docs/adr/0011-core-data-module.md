# 0011. A dedicated :core:data module for repository implementations

- Status: accepted
- Date: 2026-05-31

## Context and problem statement

`ChannelRepository` is declared in `:core:common` (domain). Its implementation
needs both `:core:database` (DAO) and `:core:network` (OkHttp + M3U parser), and
it is consumed by more than one feature: `:feature:channels` (list/search) and
`:feature:onboarding` (trigger the initial refresh after credentials arrive),
with `:feature:player` potentially looking up a channel by id.

If the implementation lived inside `:feature:channels`, the other features would
have to depend on it — but feature-to-feature dependencies are forbidden by our
layer rules (ADR-0003), which would force duplication or an illegal edge.

## Considered options

- A dedicated **`:core:data`** module implementing the repositories.
- Implementation inside `:feature:channels`.
- Implementation inside `:core:database` (no network access there).

## Decision

Add **`:core:data`**, depending on `:core:common`, `:core:database`, and
`:core:network`. It implements `ChannelRepository` and exposes an Android-facing
`ChannelPagingRepository` (Paging stays out of pure-Kotlin `:core:common`). DI
binds the implementation; every feature depends on `:core:data`, never on
another feature.

## Consequences

- Positive: one place for data orchestration; no feature-to-feature coupling;
  honours Clean Architecture (data layer implements domain interfaces).
- Positive: the streaming-import + batching logic lives with the other data code
  and is unit-testable in isolation (`PlaylistImporter`).
- Negative: one more module. Acceptable and consistent with the `:core:*` split.
