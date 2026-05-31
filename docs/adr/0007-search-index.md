# 0007. SQLite FTS for channel search

- Status: accepted
- Date: 2026-05-31

## Context and problem statement

Users search across **thousands** of channels with a D-pad. We need instant
prefix search without loading the catalogue into RAM.

## Considered options

- **SQLite FTS** (FTS4/FTS5 via Room) with prefix queries.
- A custom in-memory trie / inverted index.
- Naive `LIKE '%term%'` over the channels table.

## Decision

**SQLite FTS**, exposed through Room as an FTS-backed table synchronised with the
channels table, queried with prefix terms (`term*`). It lives on disk, integrates
with Paging 3, and adds no steady-state heap cost — unlike an in-memory trie,
which would compete for the very RAM we are trying to conserve. `LIKE '%…%'` is
rejected (no index use, full scans).

A custom trie remains a fallback **only** if profiling later shows FTS prefix
latency is unacceptable on target hardware; that would be a new ADR.

## Consequences

- Positive: O(1) heap cost, fast prefix search, pages naturally, little code.
- Negative: FTS table must be kept in sync with channel writes (trigger or
  explicit batch upsert). Acceptable and well-trodden.
