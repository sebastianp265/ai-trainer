# AI Trainer — Android app

Personal, single-user Android app that acts as an AI personal trainer. It collects my gym workouts, daily calorie burn and body weight (Polar), calorie intake (Fitatu via Health Connect), plus my preferences, and uses the Claude API to generate and adjust a flexible training + nutrition plan.

I'm an experienced software engineer but **new to Android**. When you introduce an Android-specific concept (lifecycle, ViewModel, WorkManager, Koin modules and scopes, Health Connect permissions, KSP, etc.), briefly explain what it is and why it's needed in your reply, not in code comments. Prefer clear, conventional code over clever code.

## Current phase

**Phase 3: Health Connect integration.** The product spec (phases, screens, sync, Claude integration, domain calculations) is in `docs/spec.md`. Read its relevant sections before starting a phase or a feature in that area. Finish each phase in a working, runnable state (builds, tests pass, app launches), then summarize what was done and what I should check manually.

`docs/status.md` tells me how to run the app, what works and what doesn't. Update it at the end of each phase and whenever a change affects what I'd see when running the app (new or removed screens/features, known bugs, things to verify manually).

## Project setup

Gradle/AGP setup notes: `docs/build-setup.md`. Read it before changing build files or versions.

## Hard constraints

- **No backend / no remote storage of my data.** All data lives on the device (Room). Do not add Firebase, Supabase, analytics, crash reporting or any server component.
- The only network calls allowed are:
  1. **Polar AccessLink API** (OAuth 2.0) — to read my daily activity / calories and body weight.
  2. **Claude Messages API** (`https://api.anthropic.com/v1/messages`) — for plan generation. Send compact computed summaries, never raw full history or identifying info.
- **Android cloud backup stays disabled** (`android:allowBackup="false"`, plus `dataExtractionRules` excluding everything). Manual DB export/import instead.
- Single user, no accounts, no login of our own. I may share the app with a few similar users later, so don't hardcode anything specific to me: each user enters their own API key and Polar credentials in Settings.

## Secrets handling (important)

- **No secrets anywhere in the project directory.** The Claude API key, Polar client ID/secret and Polar tokens are entered at runtime in Settings and stored on the device only, via `SecretStore`.
- Do **not** use the `local.properties` → `BuildConfig` pattern for keys, do not create `.env` files, and do not hardcode keys, even temporarily or in tests. Tests use fake values like `"test-key"`.
- Never ask me to paste a key into the chat. Use placeholders like `<CLAUDE_API_KEY>`.
- HTTP logging (Ktor `Logging` plugin) is **debug builds only** and must sanitize secrets: `sanitizeHeader { it == "x-api-key" || it == HttpHeaders.Authorization }`. Never log request bodies containing tokens (e.g. the Polar OAuth token exchange) or the contents of the encrypted preferences.
- Don't print, dump or read the app's private storage (e.g. `adb shell run-as`, `adb backup`) unless I explicitly ask.

## Tech stack

- Kotlin, **Jetpack Compose** + **Material 3** (default theme, dynamic color, dark mode), **Compose Navigation**
- **Room** (Kotlin Flow for observing data), **KSP** — not kapt
- **Ktor client** + **kotlinx.serialization**; **kotlinx-datetime** in shared code
- **Koin** for DI (`koin-android`, `koin-androidx-compose`, `koin-androidx-workmanager`), via Koin's BOM
- **Health Connect** client; approved for later: **WorkManager** (background workers), **Vico** (charts)
- **Secret storage**: Jetpack **DataStore** + an AES-256-GCM key in the **Android Keystore** (non-exportable); each value encrypted with a random IV stored alongside. Do **not** use `androidx.security:security-crypto` / EncryptedSharedPreferences (deprecated).
- **minSdk 34**, target/compile latest stable. My phone runs Android 16. Don't add version checks or compatibility code for APIs below 34.
- Single module, package-by-feature
- Also approved: `ktor-client-okhttp`, `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json`, `ktor-client-logging`, `androidx.compose.material:material-icons-core` (frozen at 1.7.x, fine), `lifecycle-runtime-compose`, `lifecycle-viewmodel-compose`
- Test-only: `kotlin-test-junit`, `koin-test-junit4`, `kotlinx-coroutines-test`, `konsist` (architecture tests), `androidx.room:room-testing` (for migration tests, later), AndroidX Test (`androidx.test.ext:junit`, Espresso, Compose `ui-test-junit4`, UI Automator for system screens such as the Health Connect permission dialog)

**Ask before adding a library not listed here.**

## Architecture

Package-by-feature with Modulith-style boundaries, enforced by Konsist in `app/src/test/.../app/ArchitectureUTest.kt`.

```
app/            MainActivity, AITrainerApp, navigation, AppDatabase, AppModules (wires everything; may import anything)
core/           shared kernel used by every feature; imports no feature and not app/
                ui/ (theme, components, formatters), db/ (TimeConverters), secrets/, CoreModule (Clock, Json, HttpClient, DataStores)
feature/<name>/ one vertical slice: domain/, data/, ui/, plus <Name>Module.kt (its Koin module)
```

Features: `exercise` (library), `workout` (logging), `health` (Health Connect), `progress` (stats and charts), `settings` (app settings, Settings screen), `plan` (Claude client, prompts, plan generation and the Plan screen); later `polar`, and `profile` (goal, training preferences, exercise preferences: removed for now, to be re-added). A feature is a business capability that owns its data and rules, not a single screen: add code to an existing feature unless it's a new capability. Rules:

- **Allowed dependencies only.** A feature may use another feature only if `allowedFeatureDependencies` in `ArchitectureUTest` allows it. That map is the only list of feature dependencies (don't copy it into docs); the test also fails on cycles and on feature packages missing from the map. When a new dependency is needed, add it to the map and mention it in your reply.
- **Public API = `domain` and `ui`.** Never import another feature's `data` or its Koin module. The one exception: a `data` package may import an allowed feature's `…Entity` for a Room foreign key.
- **Shared code goes to `core/` only if it's feature-agnostic** (formatting, theme, secrets, HTTP). Something that belongs to one feature but is used by others stays in that feature's `domain`/`ui` (e.g. exercise labels in `feature/exercise/ui/ExerciseLabels.kt`).
- **New tables**: the entity, DAO and Room repository go in the owning feature's `data/`; register the entity and DAO in `app/AppDatabase.kt` and the DAO provider in `app/AppModules.kt`.

Inside each feature, one-way dependencies: `ui → domain → data`; `domain` and `data` never import `ui`.

| Package in a feature | Contains | Rule |
|---|---|---|
| `ui/` | `XxxScreen` / `XxxContent`, `XxxViewModel`, `XxxUiState`, label helpers (`…Labels.kt`) | Shows state and forwards user actions. A ViewModel calls a repository directly only to observe data for display or to pass a user edit through unchanged; everything else goes through a use case. UI-only rules (sorting/searching what a screen shows, using translated text) are `XxxRules` here (e.g. `feature/exercise/ui/ExerciseListRules`). |
| `domain/usecase/` | `VerbNounUseCase` with one `operator fun invoke` | One action: orchestrates repositories and data sources and applies domain rules. |
| `domain/` | `XxxRepository` interfaces, `XxxRules` (pure rule functions) | Rules: calculations, no I/O; these get the unit tests. |
| `domain/model/` | Domain types (plain nouns) | Shared by all layers and, through the public API, by other features. |
| `data/` | `RoomXxxRepository`, `…Entity`, `…Dao`, `…Converters` | Storage only (observe / get / save / replace / delete, entity ↔ model mapping, transactions). No business rules. |
| `data/` of an integration feature | `XxxDataSource` / `XxxClient` interfaces + implementations, `XxxDto` | Talks to one external system and returns its data as DTOs. Other features reach it only through the feature's use cases. |
| `sync/` (when needed) | `XxxWorker`, `XxxScheduler` | Android glue: a worker only calls a use case. |

Naming says what a type is: `…Entity` (Room table), `…Row` (query projection), `…Dto` (external system data), plain noun (domain value), `…UiState` (screen state), `…UseCase` (an action with logic), `…Rules` (pure rule functions, no I/O), `…Repository`, `…DataSource` / `…Client`, `…Callback` (framework callback, e.g. Room), `…Worker`, `…Scheduler`. Every file name should say what kind of thing it holds.

External integrations sit **behind interfaces** (`ClaudeClient`, `PolarClient`, `HealthDataSource`, `SecretStore`) with real and fake implementations, so the app can run offline and without spending API credits.


**No demo or fake data in `src/main`.** Development fakes (e.g. a fake integration that fills the charts on the emulator) live only in `src/debug/`, so release builds never contain them. The exercise library (`ExerciseLibrary`) is the app's reference data, not demo data.

### Multiplatform-ready (Android-only for now)

I may later add a web or desktop version with Kotlin Multiplatform. Keep that migration cheap:

- Every feature's `domain/` (including repository interfaces) and the Claude client in `feature/plan/data/` must be **pure Kotlin**: no `android.*` / `androidx.*` imports, no `java.time` (use kotlinx-datetime), no `Context`.
- Prefer multiplatform-capable libraries for anything that might be shared.
- Android-only capabilities (Health Connect, WorkManager, Keystore secrets, Custom Tabs / OAuth redirect, file export/import) sit behind interfaces in Android-specific packages.
- Don't move to a multiplatform module structure yet.

## Data invariants

- Room schema export writes `app/schemas/<database class>/<version>.json` (the folder is named after `AppDatabase`'s fully qualified name, so moving that class creates a new folder: delete the old one). **Commit `app/schemas/`**.
- **Prototype: no migrations.** On a schema change, bump the database version (the DB is recreated). Details and the end-of-prototype steps: `docs/prototype-database.md`.
- `set_entry.positionInSession` is the set's position within the whole session, not within the exercise.
- Dates are stored as ISO text, instants as epoch millis, enum lists as comma-separated names (`core/db/TimeConverters.kt`, `feature/exercise/data/ExerciseConverters.kt`).
- Exercises are identified by `exercise.key` (`ExerciseKey` enum), the stable id used in code and later by Claude. The DB stores no exercise names. The built-in library (`ExerciseLibrary`) is inserted once, when the database is created (Room callback), never on app start.
- Health Connect data is not stored in the app: it's read on demand each time it's shown.
- Domain arithmetic (stats, TDEE, trends) is computed locally in `domain/`, never by Claude.

## Localization

The app follows the device language: **English** (`res/values/`, fallback) and **Polish** (`res/values-pl/`). Languages are listed in `res/xml/locales_config.xml` (manifest `android:localeConfig`); add new ones there. Don't use AGP's `generateLocaleConfig`.

- Every user-facing string (including content descriptions and snackbar messages) goes in `strings.xml`, in English and Polish. No hardcoded UI text in Kotlin. Lint's `MissingTranslation` must stay clean.
- Counts use `<plurals>`. Numbers and dates are formatted for the current locale (`ui/Localization.kt`), e.g. `7,5` in Polish.
- ViewModels expose typed messages (e.g. a sealed `LogMessage`), not text; the UI turns them into strings.
- Exercise names come only from string resources: `ExerciseKey.labelRes()` (exhaustive `when`, so a new key without a translation doesn't compile). Display via `Exercise.displayName(resources)`; sort by the displayed name. Search matches the displayed (native) name first, then the English name, for every supported locale.
- Enums are shown through string resources (e.g. `MuscleGroup.labelRes()`), never `enum.name`.

## Testing

- **Prefer integration tests that follow a user flow** in `androidTest/` (see `app/src/androidTest/CLAUDE.md`). Fakes only at external boundaries (Claude, Polar, Health Connect), never for our own classes.
- **Integration data enters tests only through its fake.** Data that comes from Health Connect, Polar or Claude is provided by the fake at that boundary (e.g. `FakeHealthDataSource`) and flows through the real read path into the app, so the whole path is tested. Never write it into the database directly. Our own data (workouts, settings) may be set up in the database as a test's starting point, once another test covers creating it through the UI.
- **Keep unit tests few and meaningful** (`./gradlew test`): only for non-trivial pure logic where a bug would be subtle (domain stats, plan JSON validation). No coverage-only tests (mappings, converters, getters, data lists). Don't mock our own code.
- **Expected values are literals, never computed.** Decide the exact expected text/numbers/order first and write it into the test. Don't build expected values from string resources, plural rules, formatters or the production code under test.
- Naming: integration tests end with `ITest`, unit tests with `UTest`.
- Use **`kotlin.test`** on JUnit 4 in both `test/` and `androidTest/`. No JUnit 5. `org.junit` only for `@RunWith` and `@get:Rule`.
- A Koin unit test verifies the module graph, and `ArchitectureUTest` (Konsist) verifies the package rules; keep both passing.
- Run the relevant unit tests (`./gradlew testDebugUnitTest`) after changes, and `./gradlew assembleDebug` after each meaningful change.
- **Don't run instrumented tests (`…ITest`, `connectedDebugAndroidTest`) unless I ask you to.** Compiling them (`compileDebugAndroidTestKotlin`) is fine and expected.
- Instrumented tests uninstall the app afterwards, deleting its data, so run them **on the emulator, not on my phone**. When I ask for them, run them from the shell, always pinned to the emulator:
  - all: `ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest`
  - one test: append `-Pandroid.testInstrumentationRunnerArguments.class=<fully.qualified.TestClass>#<testMethod>` (or just the class)
  - Each exact command must be in `.claude/settings.local.json` before running it. On failure, the report is in `app/build/reports/androidTests/connected/`.
  - Don't judge instrumented tests from Android Studio's runner: it reports exit code 0 even when a test fails.

## Conventions

- Kotlin coding conventions; small composables; UI state as immutable data classes exposed via `StateFlow`.
- **One top-level type per file, named after it**, so classes can be found by file name. Exception: a small type that exists only for that one type (e.g. `Secret` for `SecretStore`, `SessionSummaryRow` returned by a DAO method, a screen's `…UiState` / `…Tags` / `…Actions`). Mapping functions live in the file that uses them.
- Screens: a stateful `XxxScreen` (gets the ViewModel from Koin) wraps a stateless `XxxContent` (state + callbacks). Add `@Preview`s with fake state for the main screens, placed at the top of the file right after the imports, followed by the helpers only they use (sample data, no-op actions).
- User edits that must not be lost when the screen closes (settings, secrets) are saved with `NonCancellable` (see `SettingsViewModel.saveEvenIfScreenIsLeft`).
- **No comments in code** (Kotlin, XML, Gradle, tests). When something isn't clear, rename, extract a well-named function/constant/type, or restructure. Explain Android concepts and design reasons in your reply.
- Commit-sized changes; don't refactor unrelated code.
- Never commit secrets, keystores or `local.properties`.
