# AI Trainer — Android app

Personal, single-user Android app that acts as an AI personal trainer. It collects my gym workouts, daily calorie burn and body weight (Polar), calorie intake (Fitatu via Health Connect), plus my preferences, and uses the Claude API to generate and adjust a flexible training + nutrition plan.

I'm an experienced software engineer but **new to Android**. When you introduce an Android-specific concept (lifecycle, ViewModel, WorkManager, Koin modules and scopes, Health Connect permissions, KSP, etc.), briefly explain what it is and why it's needed in your reply, not in code comments. Prefer clear, conventional code over clever code.

## Current phase

**Phase 3: Health Connect + progress.** The product spec (phases, screens, sync, Claude integration, domain calculations) is in `docs/spec.md`. Read its relevant sections before starting a phase or a feature in that area. Finish each phase in a working, runnable state (builds, tests pass, app launches), then summarize what was done and what I should check manually.

`docs/status.md` tells me how to run the app, what works and what doesn't. Update it at the end of each phase and whenever a change affects what I'd see when running the app (new or removed screens/features, known bugs, things to verify manually).

## Project setup

Created from Android Studio's **Empty Activity (Compose)** template. Keep the generated Gradle wrapper, AGP and Kotlin setup; extend it rather than regenerating it. Add dependencies through the version catalog (`gradle/libs.versions.toml`).

- AGP 9 uses **built-in Kotlin**: there is no `kotlin-android` plugin (don't add one), and Kotlin compiler options go in a top-level `kotlin { compilerOptions { } }` block.
- The `kotlin` catalog version (shared by the Compose and serialization plugins) was raised from 2.2.10 to 2.4.21 because Ktor 3.6, kotlinx.serialization 1.12 and Koin 4.2 need it. Don't lower it.
- KSP versions (2.3.0+) are independent of the Kotlin version.

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
- **WorkManager**, **Health Connect** client, **Vico** for charts
- **Secret storage**: Jetpack **DataStore** + an AES-256-GCM key in the **Android Keystore** (non-exportable); each value encrypted with a random IV stored alongside. Do **not** use `androidx.security:security-crypto` / EncryptedSharedPreferences (deprecated).
- **minSdk 34**, target/compile latest stable. My phone runs Android 16. Don't add version checks or compatibility code for APIs below 34.
- Single module, package-by-feature
- Also approved: `ktor-client-okhttp`, `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json`, `ktor-client-logging`, `androidx.compose.material:material-icons-core` (frozen at 1.7.x, fine), `lifecycle-runtime-compose`, `lifecycle-viewmodel-compose`
- Test-only: `kotlin-test-junit`, `koin-test-junit4`, `kotlinx-coroutines-test`, `androidx.room:room-testing`, AndroidX Test (`androidx.test.ext:junit`, Espresso, Compose `ui-test-junit4`)

Use current stable versions compatible with the existing AGP/Kotlin; check rather than guess. **Ask before adding a library not listed here.**

## Architecture

MVVM + repository: `ui (Compose) → ViewModel (StateFlow UI state) → Repository → { Room DAO | API client | Health Connect client }`.

External integrations sit **behind interfaces** (`ClaudeClient`, `PolarClient`, `HealthDataSource`, `SecretStore`) with real and fake implementations, so the app can run offline and without spending API credits.

Packages under the app package: `data/` (`db`, `repo`, `secrets`, `healthconnect`, `demo`, later `polar`, `claude`), `domain/` (`model`, `workout`, `stats`), `sync/`, `ui/` (one package per screen, plus `navigation`, `components`, `theme`), `di/`. The `debug` screen is compiled into all builds but reachable only when `BuildConfig.DEBUG`.

### Multiplatform-ready (Android-only for now)

I may later add a web or desktop version with Kotlin Multiplatform. Keep that migration cheap:

- `domain/`, `data/claude/` and the repository interfaces must be **pure Kotlin**: no `android.*` / `androidx.*` imports, no `java.time` (use kotlinx-datetime), no `Context`.
- Prefer multiplatform-capable libraries for anything that might be shared.
- Android-only capabilities (Health Connect, WorkManager, Keystore secrets, Custom Tabs / OAuth redirect, file export/import) sit behind interfaces in Android-specific packages.
- Don't move to a multiplatform module structure yet.

## Data invariants

- Room schema export writes `app/schemas/<version>.json`. **Commit `app/schemas/`**. Write migrations, never destructive fallback.
- `set_entry.positionInSession` is the set's position within the whole session, not within the exercise.
- `profile_prefs` is a single-row table (`id = 1`).
- Dates are stored as ISO text, instants as epoch millis, enum lists as comma-separated names (`data/db/Converters.kt`).
- Seed exercises (`SeedExercises`) are inserted on every app start with "ignore if the name exists", so user preferences are never overwritten. **Don't rename seed exercises**: a renamed one is inserted again as a duplicate.
- Burned calories: if both Polar and Health Connect provide them, Polar wins; record the source.
- Health Connect is the source of truth for intake/macros and the burned calories it provided: every sync replaces each day of the sync window with what Health Connect reports, so entries deleted there disappear from the app too. Polar or manual burned calories are never removed by a Health Connect sync.
- Domain arithmetic (stats, TDEE, trends) is computed locally in `domain/`, never by Claude.

## Localization

The app follows the device language: **English** (`res/values/`, fallback) and **Polish** (`res/values-pl/`). Languages are listed in `res/xml/locales_config.xml` (manifest `android:localeConfig`); add new ones there. Don't use AGP's `generateLocaleConfig`.

- Every user-facing string (including content descriptions and snackbar messages) goes in `strings.xml`, in English and Polish. No hardcoded UI text in Kotlin. Lint's `MissingTranslation` must stay clean.
- Counts use `<plurals>`. Numbers and dates are formatted for the current locale (`ui/Localization.kt`), e.g. `7,5` in Polish.
- ViewModels expose typed messages (e.g. a sealed `LogMessage`), not text; the UI turns them into strings.
- `exercise.name` is the stable English key (stored in the DB, used by Claude). Display via `Exercise.displayName(resources)`; sort and search by the displayed name.
- Enums are shown through string resources (e.g. `MuscleGroup.labelRes()`), never `enum.name`.

## Testing

- **Prefer integration tests that follow a user flow** in `androidTest/` (see `app/src/androidTest/CLAUDE.md`). Fakes only at external boundaries (Claude, Polar, Health Connect), never for our own classes.
- **Keep unit tests few and meaningful** (`./gradlew test`): only for non-trivial pure logic where a bug would be subtle (domain stats, plan JSON validation). No coverage-only tests (mappings, converters, getters, data lists). Don't mock our own code.
- **Expected values are literals, never computed.** Decide the exact expected text/numbers/order first and write it into the test. Don't build expected values from string resources, plural rules, formatters or the production code under test.
- Naming: integration tests end with `ITest`, unit tests with `UTest`.
- Use **`kotlin.test`** on JUnit 4 in both `test/` and `androidTest/`. No JUnit 5. `org.junit` only for `@RunWith` and `@get:Rule`.
- A Koin unit test verifies the module graph; keep it passing when adding dependencies.
- Run the relevant tests after changes, and `./gradlew assembleDebug` after each meaningful change.
- Instrumented tests (`./gradlew connectedDebugAndroidTest`) uninstall the app afterwards, deleting its data, so run them **on the emulator, not on my phone**.

## Conventions

- Kotlin coding conventions; small composables; UI state as immutable data classes exposed via `StateFlow`.
- Screens: a stateful `XxxScreen` (gets the ViewModel from Koin) wraps a stateless `XxxContent` (state + callbacks). Add `@Preview`s with fake state for the main screens.
- User edits that must not be lost when the screen closes (settings, secrets) are saved with `NonCancellable` (see `SettingsViewModel.saveEvenIfScreenIsLeft`).
- **No comments in code** (Kotlin, XML, Gradle, tests). When something isn't clear, rename, extract a well-named function/constant/type, or restructure. Explain Android concepts and design reasons in your reply.
- Commit-sized changes; don't refactor unrelated code.
- Never commit secrets, keystores or `local.properties`.
