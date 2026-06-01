# 0012. Channel-list paging and UI performance

- Status: accepted
- Date: 2026-06-01

## Context and problem statement

A real provider catalogue is ~11 000 channels. The channel home must let the user
scroll and **keypad-zap to any channel number instantly**, on a low-RAM Android TV.
Two classes of problem surfaced:

1. **Data access.** Room's generated `PagingSource` pages with `LIMIT/OFFSET`.
   `OFFSET k` makes SQLite read and discard `k` rows, so a page load near row
   6 000 re-reads ~6 000 rows — scrolling and zapping got slower the deeper you
   went. Ordering "Toutes" by a computed expression
   (`(category='Général') DESC, sortIndex`) also forced a full filesort of every
   row on every page load. A far `scrollToItem` with no jump threshold compounded
   this by loading every intervening page (~125 sequential filesorts → ~10 s).

2. **Rendering.** The fullscreen zoom animated per frame from state read at the
   top of the screen composable, so the whole screen recomposed ~60×/s; logos are
   remote and decoded on scroll; domain models live in a pure-Kotlin module the
   Compose compiler can't analyse for stability.

## Decision

**A fixed, dense `channelNumber` is the catalogue's primary ordering, and the
"Toutes" list is served by a hand-written keyset `PagingSource`.**

- `channelNumber` is assigned 1..N at import over exactly the visible set
  (primary, non-adult, generalist first then playlist order) and indexed. It is
  the channel's stable keypad number **and** its row position (`position + 1`).
- `ChannelCatalogPagingSource` pages by it: `WHERE channelNumber >= key LIMIT n`,
  an index seek + page read — **O(log n + pageSize) at any position**. It exposes
  `itemsBefore`/`itemsAfter` (placeholders) so the list can jump to any row,
  reports `jumpingSupported = true`, and invalidates on the `channels` table like
  Room's own sources.
- The list uses `enablePlaceholders = true` + a `jumpThreshold`, so a keypad zap
  `scrollToItem`s straight to the target; Paging loads only the window there.

**Rendering is kept off the hot paths:**

- The zoom's per-frame animation lives in a self-contained `FullscreenOverlay`
  composable, so only that subtree recomposes during open/close.
- Domain models (`Channel`, `ChannelId`, `NowNext`, `EpgProgramme`) are marked
  stable through a Compose stability-configuration file (they are immutable but
  unanalysable from `:core:common`).
- Coil is an app singleton with a bounded on-disk logo cache and capped decode
  parallelism; placeholder rows are row-shaped skeletons sharing the row content
  type for slot reuse.
- The home player has a dedicated lighter buffer profile (faster first frame,
  lower memory ceiling) since it is also the side preview that morphs to
  fullscreen.

## Considered options

- **Room `LIMIT/OFFSET` everywhere.** Simplest, auto-invalidated, jump-capable —
  but O(offset) reads make deep scroll/jump slow on a large list.
- **Placeholders + indexed `ORDER BY` + jumpThreshold, still on Room.** Removes
  the filesort and collapses a far jump to one load, but the single jump load
  still does an O(offset) read.
- **Custom keyset `PagingSource` (chosen)** for "Toutes": O(pageSize) at any
  position, no OFFSET.

## Consequences

- Positive: scrolling and zapping are flat-cost across the whole catalogue.
- Positive: the fixed `channelNumber` doubles as the keypad number and the scroll
  index, so a zap maps to a row with no extra lookup.
- Negative: a custom `PagingSource` must wire its own Room invalidation (done via
  `InvalidationTracker`), and `:core:data` now depends on `room-runtime`.
- Negative: keyset assumes a **dense** key. **Category** and **search** lists keep
  Room's `OFFSET` source: their keys aren't dense and they are smaller subsets, so
  the cost is acceptable and a correct keyset there (non-dense keys, bidirectional
  prepend) isn't worth a schema change + re-import yet. Tracked as a follow-up.
