# core — the code shared by every platform

`:core:common` and `:core:android` together are everything except the frontend. It runs on the device; nothing is remote.

## Multiplatform (JVM target only for now)

I may later add an iOS, web or desktop version, as extra targets of `:core:common` plus a platform module like `:core:android`. Keep that cheap:

- Services, models and interfaces go in `:core:common` and must be **pure Kotlin** (use kotlinx-datetime, not `java.time`; no `android.*`, no `java.*`).
- Prefer multiplatform-capable libraries for anything in `:core:common` (Room, DataStore, Koin and kotlinx are).
- Android-only capabilities (Health Connect, WorkManager, Keystore secrets, file export/import, the AI client, Claude on the official Java SDK) sit behind interfaces declared in `:core:common`'s `integration` and implemented in `:core:android`, or in `androidApp` when they're UI (e.g. permission screens). `:core:android` reaches `:core:common` only through its `integration`.

## Services

- A service is `DefaultXService` in `feature.<domain>.impl`.
- No repository interfaces or use cases: `DefaultXService` uses DAOs, DataStore and `SecretStore` directly. Exception: `HealthDataSource` (external system).
