# Architecture Decision Records

These ADRs capture the structural decisions behind Strix, in
[MADR](https://adr.github.io/madr/) format. They are numbered sequentially and
immutable once `accepted`; a later ADR supersedes an earlier one rather than
editing it.

| ADR | Title | Status |
| --- | ----- | ------ |
| [0001](0001-kotlin-only.md) | Kotlin only, no native code | accepted |
| [0002](0002-clean-architecture-and-mvi.md) | Clean Architecture + MVI | accepted |
| [0003](0003-multi-module-structure.md) | Multi-module Gradle structure | accepted |
| [0004](0004-media3-over-custom-player.md) | Media3/ExoPlayer over a custom player | accepted |
| [0005](0005-local-server-onboarding.md) | Local-server (no-cloud) QR onboarding | accepted |
| [0006](0006-onboarding-frontend.md) | Single self-contained HTML onboarding page | accepted |
| [0007](0007-search-index.md) | SQLite FTS for channel search | accepted |
| [0008](0008-embedded-http-server.md) | NanoHTTPD for the embedded server | accepted |
| [0009](0009-dependency-injection.md) | Hilt for dependency injection | accepted |
| [0010](0010-build-logic-convention-plugins.md) | build-logic convention plugins | accepted |
| [0011](0011-core-data-module.md) | A dedicated :core:data module | accepted |

To add one, copy [`template.md`](template.md).
