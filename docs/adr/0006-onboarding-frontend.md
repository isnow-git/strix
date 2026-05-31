# 0006. Single self-contained HTML onboarding page

- Status: accepted
- Date: 2026-05-31

## Context and problem statement

The phone-side onboarding form is served by the TV over the LAN and must work
**fully offline** (no internet assumed). We need to choose how to build it.

## Considered options

- **One self-contained HTML file** (inline CSS/JS), shipped in `assets/`.
- A bundled SPA (Vue/React via Vite) in `assets/`.
- A CDN-hosted frontend.

## Decision

**A single self-contained HTML file**, mobile-first, auto dark-mode, with care
for focus states and a success transition. It is served straight from the TV's
`assets/` and needs no network beyond the LAN.

A **CDN dependency is explicitly rejected** — it would break the offline LAN
guarantee. If the flow ever needs a richer wizard, we will **bundle Vue/React via
Vite into `assets/`** (still no CDN), recorded as a follow-up ADR.

## Consequences

- Positive: trivially offline, tiny payload, nothing to build for the simple case.
- Positive: easy to audit (one file) — relevant since it handles credentials.
- Negative: hand-written HTML/JS doesn't scale to a complex UI; the Vite-bundle
  escape hatch is pre-approved for that case.
