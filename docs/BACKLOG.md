# Backlog

One issue per unit of work, created on GitHub once the remote exists. Until
then this file is the source of truth. Suggested labels and milestones are
listed; create the labels first, then the issues.

## Milestones

1. **M1 – Foundation** : repo skeleton, CI, `core:common`, `core:database`.
2. **M2 – Data** : `core:network` (parser, resilience), `core:player` (Media3 wrapper).
3. **M3 – Playback MVP** : `feature:channels`, `feature:player` end-to-end.
4. **M4 – Onboarding** : QR + embedded server + encrypted credentials.
5. **M5 – Polish** : `feature:epg`, perf passes, R8, docs.

## Labels

`area:app` `area:core-common` `area:core-ui` `area:core-database`
`area:core-network` `area:core-player` `area:feature-channels`
`area:feature-player` `area:feature-onboarding` `area:feature-epg`
`area:build` · `type:feat` `type:chore` `type:docs` · `perf` `good first issue`

## Issues

### Foundation (M1)
- **#: Repo skeleton + Gradle multi-module + convention plugins** — `area:build` (this scaffold). *Done.*
- **#: CI pipeline (build, tests, ktlint, detekt, APK artifact)** — `area:build`. *Done.*
- **#: `core:common` — domain models, `Result`, `DispatcherProvider`, interfaces** (`ChannelRepository`, `StreamSource`, `CredentialReceiver`) — `area:core-common` `type:feat`.
- **#: `core:database` — Room entities, DAOs, batched playlist insert** — `area:core-database` `type:feat` `perf`.
- **#: `core:database` — FTS table + prefix search query (ADR-0007)** — `area:core-database` `perf`.

### Data (M2)
- **#: `core:network` — OkHttp client (connection pool, timeouts)** — `area:core-network` `type:feat`.
- **#: `core:network` — streaming M3U parser (O(1) memory) → batch sink** — `area:core-network` `perf`.
- **#: `core:network` — EPG (XMLTV) streaming parser** — `area:core-network` `perf`.
- **#: `core:network` — retry (exp backoff + jitter) + circuit breaker** — `area:core-network` `type:feat`.
- **#: `core:player` — Media3 wrapper, lifecycle-bound release** — `area:core-player` `perf`.
- **#: `core:player` — custom `LoadControl` + ABR policy for low TV RAM** — `area:core-player` `perf`.

### Playback MVP (M3)
- **#: `core:ui` — Compose TV theme + D-pad focus helpers** — `area:core-ui` `type:feat`.
- **#: `feature:channels` — Paging 3 list, MVI ViewModel, Coil logos (downsampled)** — `area:feature-channels` `perf`.
- **#: `feature:channels` — search UI wired to FTS** — `area:feature-channels`.
- **#: `feature:channels` — debounced + `collectLatest` zapping** — `area:feature-channels` `perf`.
- **#: `feature:player` — fullscreen playback screen + controls** — `area:feature-player` `type:feat`.
- **#: LRU bitmap/cache sizing from `ActivityManager.memoryClass`** — `area:core-ui` `perf`.

### Onboarding (M4)
- **#: `feature:onboarding` — NanoHTTPD embedded server, lifecycle-bound (ADR-0005/0008)** — `area:feature-onboarding` `type:feat`.
- **#: `feature:onboarding` — LAN IP discovery via `NetworkInterface`** — `area:feature-onboarding`.
- **#: `feature:onboarding` — QR generation (ZXing) + token (single-use, TTL)** — `area:feature-onboarding`.
- **#: `feature:onboarding` — self-contained HTML form in assets (ADR-0006)** — `area:feature-onboarding`.
- **#: `feature:onboarding` — encrypted credential storage + provider test** — `area:feature-onboarding` `type:feat`.

### Polish (M5)
- **#: `feature:epg` — program guide grid** — `area:feature-epg` `type:feat`.
- **#: R8 full-mode pass + keep-rule audit** — `area:build` `perf`.
- **#: Perf pass — Compose stability, allocation audit on hot paths** — `perf`.
- **#: Screenshots / GIF + README polish** — `type:docs`.

## Creating these on GitHub

```bash
# After `gh repo create` / adding a remote:
gh label create "area:core-network" --color 1d76db
# …repeat per label…
gh issue create --title "core:network — streaming M3U parser (O(1) memory)" \
  --label "area:core-network,perf" --milestone "M2 – Data" \
  --body "See docs/BACKLOG.md and docs/adr/."
```
