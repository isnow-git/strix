# 0005. Local-server (no-cloud) QR onboarding

- Status: accepted
- Date: 2026-05-31

## Context and problem statement

Typing an M3U URL or Xtream host/user/password with a TV remote is painful. We
want a smooth handoff to the phone **without** running any cloud service (privacy,
zero ops cost, works on a home LAN with no account).

## Considered options

- **Embedded HTTP server on the TV** + QR; the phone loads a form served by the
  TV and POSTs credentials back.
- A hosted cloud relay/pairing service.
- Manual entry only.

## Decision

**Embedded local server + QR.** During onboarding only, the TV starts an HTTP
server on an **ephemeral port** bound to the **site-local interface**, shows a QR
encoding `http://<lan-ip>:<port>/?t=<token>`. The phone scans it, the form is
served by the TV, and credentials are POSTed back, then stored **encrypted**.
The TV ideally tests the provider connection before finishing.

Lifecycle & security:

- Server is tied to the onboarding screen via `DisposableEffect`; `stop()` on
  `onDispose`, fully GC'd afterwards → **zero cost in normal operation**.
- Token is **single-use**, **short TTL**, accepts **one** submission, server is
  bound to the site-local interface only.
- Plain HTTP is acceptable on a home LAN; show a clear message under **AP
  isolation**. Outbound cleartext to `http://` providers is allowed via
  `network-security-config`.
- LAN IP discovery enumerates `NetworkInterface` (covers Ethernet **and** Wi-Fi),
  picking the site-local IPv4.

## Consequences

- Positive: no backend, no accounts, no recurring cost; data stays on the LAN.
- Positive: no runtime footprint outside onboarding.
- Negative: AP isolation / guest networks can block phone→TV reachability; we
  detect and explain it. Plain HTTP on LAN is a deliberate, scoped trade-off.
