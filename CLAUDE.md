# AI Trainer — Android app

Personal, single-user Android app that acts as an AI personal trainer. It collects my gym workouts, daily calorie burn and body weight (Polar), calorie intake (Fitatu via Health Connect), plus my preferences, and uses the Claude API to generate and adjust a flexible training + nutrition plan.

I'm an experienced software engineer but **new to Android**. When you introduce an Android-specific concept (lifecycle, ViewModel, WorkManager, Koin modules and scopes, Health Connect permissions, KSP, etc.), briefly explain what it is and why it's needed in your reply, not in code comments. Prefer clear, conventional code over clever code.

## Project state

The project was created with Android Studio's **Empty Activity (Compose)** template (Kotlin DSL, minSdk 34). Keep the generated Gradle wrapper, Android Gradle Plugin and Kotlin setup; extend it rather than regenerating it. Add dependencies through the version catalog (`gradle/libs.versions.toml`).

- AGP 9 uses **built-in Kotlin**: AGP compiles Kotlin itself, so there is no `kotlin-android` plugin (don't add one) and Kotlin compiler options go in a top-level `kotlin { compilerOptions { } }` block.
- The Kotlin version (`kotlin` in the catalog, shared by the Compose and serialization plugins) may be raised above AGP's default when libraries require it. It was raised from 2.2.10 to 2.4.21 in phase 1 because Ktor 3.6, kotlinx.serialization 1.12 and Koin 4.2 are compiled with Kotlin 2.3/2.4. Don't lower it.
- KSP versions (2.3.0+) are independent of the Kotlin version.

## Hard constraints

- **No backend / no remote storage of my data.** All data lives on the device (Room). Do not add Firebase, Supabase, analytics, crash reporting or any server component.
- The only network calls allowed are:
  1. **Polar AccessLink API** (OAuth 2.0) — to read my daily activity / calories and body weight.
  2. **Claude Messages API** (`https://api.anthropic.com/v1/messages`) — for plan generation. Send compact computed summaries, never raw full history or identifying info.
- **Disable Android cloud backup** for the app (`android:allowBackup="false"`, plus `dataExtractionRules` excluding everything). Provide manual DB export/import instead.
- Single user, personal use only. No accounts, no login of our own. I may share the app with a few similar users later, so don't hardcode anything specific to me: each user enters their own API key and Polar credentials in Settings.

## Localization

The app follows the device language. Supported: **English** (`res/values/`, the fallback for any unsupported language) and **Polish** (`res/values-pl/`). Supported languages are listed in `res/xml/locales_config.xml` (referenced from the manifest via `android:localeConfig`), so the app appears in Android's per-app language settings; add new languages there. Don't use AGP's `generateLocaleConfig` (incubating API).

- Every user-facing string (including content descriptions and snackbar messages) goes in `strings.xml`, with both an English and a Polish version. No hardcoded UI text in Kotlin. Lint's `MissingTranslation` must stay clean.
- Counts use `<plurals>` (Polish has one/few/many/other forms). Numbers and dates are formatted for the current locale (`ui/Localization.kt`: `formatWeight`, `formatDate`), e.g. `7,5` in Polish.
- ViewModels expose typed messages (e.g. a sealed `LogMessage`), not text; the UI turns them into strings.
- Exercise names: `exercise.name` is the stable English key (stored in the DB, used by Claude). Seed exercises have a translated name resource (`SeedExercises.translatedNameRes`), shown via `Exercise.displayName(resources)`. Sort and search by the displayed name.
- Muscle groups and other enums are shown through string resources (e.g. `MuscleGroup.labelRes()`), never `enum.name`.
- Claude (phase 5): ask for free text (rationale, notes, "adjust today" replies) in the app's current language; exercise names in the plan JSON stay the English keys.

## Secrets handling (important)

- **No secrets anywhere in the project directory.** The Claude API key, Polar client ID/secret and Polar tokens are entered by me at runtime on the Settings screen and stored on the device only, encrypted with a key held in the Android Keystore (see `SecretStore` below).
- Do **not** use the `local.properties` → `BuildConfig` pattern for keys, do not create `.env` files, and do not hardcode keys, even temporarily or in tests. Tests use fake values like `"test-key"`.
- Never ask me to paste a key into the chat. If you need to refer to one, use placeholders like `<CLAUDE_API_KEY>`.
- HTTP logging (Ktor `Logging` plugin) is **debug builds only** and must sanitize secrets: `sanitizeHeader { it == "x-api-key" || it == HttpHeaders.Authorization }`. Never log request bodies containing tokens (e.g. the Polar OAuth token exchange) or the contents of the encrypted preferences.
- Don't print, dump or read the app's private storage (e.g. `adb shell run-as`, `adb backup`) unless I explicitly ask.

## Tech stack

- Kotlin, **Jetpack Compose** + **Material 3** (default theme for now, dynamic color, dark mode)
- **Room** for local storage (Kotlin Flow for observing data)
- **Ktor client** + **kotlinx.serialization** for HTTP/JSON
- **Koin** for DI (`koin-android`, `koin-androidx-compose` for `koinViewModel()`, `koin-androidx-workmanager` for workers). Use Koin's BOM in the version catalog.
- **KSP** for annotation processing (Room) — not kapt
- **kotlinx-datetime** for dates/times in shared (non-Android) code
- **WorkManager** for background sync and the weekly review
- **Health Connect** client (`androidx.health.connect:connect-client`)
- **Vico** for charts
- **Compose Navigation**
- **Secret storage**: Jetpack **DataStore** + an AES-256-GCM key generated in the **Android Keystore** (non-exportable); encrypt each value before storing it (random IV stored alongside the ciphertext). Do **not** use `androidx.security:security-crypto` / EncryptedSharedPreferences; it is deprecated.
- Gradle Kotlin DSL + version catalog
- **minSdk 34** (Android 14; Health Connect is built into the OS from here), target/compile latest stable. My phone runs Android 16. Don't add version checks or compatibility code for APIs below 34.
- Single module for now, package-by-feature

- Also approved:
  - `ktor-client-okhttp` (Ktor's HTTP engine on Android)
  - `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json`, `ktor-client-logging`
  - `androidx.compose.material:material-icons-core` (basic icons; frozen at 1.7.x, fine for now)
  - `lifecycle-runtime-compose` (`collectAsStateWithLifecycle`), `lifecycle-viewmodel-compose`
- Test-only libraries:
  - `kotlin-test-junit` (`kotlin.test`)
  - `koin-test-junit4` (Koin `verify()`)
  - `kotlinx-coroutines-test` (`runTest`)
  - `androidx.room:room-testing` (migration tests)
  - AndroidX Test (`androidx.test.ext:junit`, Espresso, Compose `ui-test-junit4`)

Use current stable versions of all libraries that are compatible with the existing AGP/Kotlin versions; check rather than guess. Ask before adding a library not listed here.

## Multiplatform-ready (Android-only for now)

The app targets Android only, but I may later add a web or desktop version with **Kotlin Multiplatform + Compose Multiplatform**. Keep that migration cheap:

- `domain/`, `data/claude/` (client, `PromptBuilder`, plan schema + validation) and the repository interfaces must be **pure Kotlin**: no `android.*` / `androidx.*` imports, no `java.time` (use kotlinx-datetime), no Android `Context`.
- Prefer multiplatform-capable libraries (Ktor, kotlinx.serialization, kotlinx-datetime, Koin, Room) for anything that might be shared.
- Android-only capabilities sit behind interfaces and live in Android-specific packages: Health Connect (`HealthDataSource`), WorkManager scheduling, encrypted secret storage (`SecretStore`), Custom Tabs / OAuth redirect handling, file export/import.
- Don't move to a multiplatform module structure yet; just respect these boundaries.

## Architecture

MVVM + repository pattern:

```
ui (Compose screens) → ViewModel (StateFlow UI state) → Repository → { Room DAO | API client | Health Connect client }
```

External integrations sit **behind interfaces** (`ClaudeClient`, `PolarClient`, `HealthDataSource`, `SecretStore`) with real and fake implementations, so the app can run offline and without spending API credits.

Package layout (under the existing app package):

```
data/
  db/             Room database, entities, DAOs, migrations, seed data
  healthconnect/  Health Connect wrapper + mappers            (Android-only)
  polar/          Polar AccessLink client, OAuth, DTOs
  claude/         Claude API client, prompt builder, plan JSON schema + validation   (pure Kotlin)
  secrets/        Encrypted secret storage                     (Android-only impl)
  repo/           Repository interfaces (pure Kotlin) + Room-backed implementations
domain/           Pure Kotlin (no Android deps → unit-testable, shareable)
  model/          Domain models and enums (MuscleGroup, Equipment, Goal, …), also used by Room entities
  workout/        Workout logic (set grouping, weight/rep steps); stats calculations get their own sub-packages
sync/             WorkManager workers                          (Android-only)
ui/
  navigation/     App shell: bottom navigation bar, type-safe @Serializable routes, NavHost
  theme/          Material 3 theme (generated by the template)
  PlaceholderScreen.kt  Temporary content for tabs not built yet
  log/            Workout logging
  plan/           Current plan + "adjust today"
  progress/       Charts and summaries
  settings/       Preferences, API keys, Polar connect, export/import
  debug/          Debug-only tools (seed demo data, toggle fakes)
di/               Koin modules (e.g. dataModule, networkModule, viewModelModule, fakesModule)
```

## Data model (Room)

- `exercise` — id, name, primaryMuscles, secondaryMuscles, equipment, preference (FAVOURITE / NEUTRAL / AVOID)
- `workout_session` — id, date, startedAt, finishedAt, planSessionRef (nullable), notes
- `set_entry` — id, sessionId, exerciseId, positionInSession, reps, weightKg, rpe (nullable), isWarmup
- `daily_energy` — date (PK), kcalBurned, kcalBurnedSource, kcalEaten, proteinG, carbsG, fatG, updatedAt
- `body_weight` — id, measuredAt, weightKg, source (POLAR / MANUAL), unique on (measuredAt, source) so repeated syncs don't duplicate entries
- `profile_prefs` — goal (CUT / MAINTAIN / BULK / RECOMP), sessionsPerWeek, sessionLengthMin, weakMuscleGroups, availableEquipment, free-text notes
- `plan` — id, version, createdAt, isActive, validFrom, validTo, json (the validated plan), rationale

Implementation notes:

- `set_entry.positionInSession` is the set's position within the whole session (not within the exercise). Schema v2 renamed it from `setOrder` via an auto-migration.
- `profile_prefs` is a single-row table (`id = 1`).
- Dates (`LocalDate`) are stored as ISO text, instants as epoch milliseconds, and enum lists as comma-separated names (see `data/db/Converters.kt`).
- The seed exercise library (`SeedExercises`) is inserted on every app start with "ignore if the name exists". New seed entries reach existing installs and user preferences are never overwritten. Don't rename seed exercises: a renamed one is inserted again as a duplicate.

Seed a reasonable default exercise library with muscle groups. Room schema export is enabled via the Room Gradle plugin and writes `app/schemas/<version>.json`. **Commit `app/schemas/`**, since migrations are checked and tested against it. Once real data exists, write migrations instead of destructive fallback.

## Domain calculations (local, deterministic, unit-tested)

Compute these in `domain/`. Claude should not do the arithmetic.

- Weekly sets per muscle group (primary = 1, secondary = 0.5)
- Adherence: planned vs completed sessions
- Weight trend: 7-day moving average and kg/week slope
- Average daily intake, average burn, average deficit/surplus
- **Empirical TDEE** from the weight trend vs logged intake over 2–4 weeks (wearable calorie burn is imprecise; prefer this when enough data exists)
- Per-exercise progression info (best set, estimated 1RM, last session's load)

## Claude integration

- Model id configurable in Settings (default: `claude-sonnet-5-5`).
- API key read from encrypted storage at call time.
- Handle errors gracefully: 401 (bad key), 429 / rate limit, spend limit or credits exhausted, network errors → show "AI trainer unavailable" with the reason, and keep the rest of the app working.
- **Plan generation**: build a compact prompt from profile_prefs + domain stats (last 4 weeks) + exercise library (filtered by preference and equipment). Require output in a strict JSON schema:
  - `weeks` → `sessions` → `exercises` → `{exerciseName, sets, repRange, targetRpe, restSec, notes}`
  - `nutrition` → `{dailyKcal, proteinG, notes}`
  - `rationale` (short text)
- Validate the response with kotlinx.serialization plus sanity checks (exercises exist in the library, sensible ranges). On failure, retry once including the validation error; then surface the error.
- **"Adjust today"**: send today's planned session plus my free-text note (e.g. "only 40 min, sore shoulder") → receive a modified session in the same schema.
- **Weekly review** (WorkManager, toggle in Settings, off by default): regenerate next week's plan from the past week's data.
- All prompts live in one place (`PromptBuilder`) so they're easy to iterate on.

## Data sync

- **Health Connect** (daily WorkManager job + on app open): read nutrition (kcal, macros) and active/total calories if present. Body weight comes from Polar, not Health Connect. Request permissions with a clear UI. Health Connect is part of the OS on API 34+, but still check `HealthConnectClient.getSdkStatus()` and handle "unavailable" / "update required" gracefully. Read-only: the app never writes to Health Connect.
- **Polar AccessLink**: OAuth 2.0 authorization code flow via Custom Tabs, with a redirect back into the app (custom scheme, e.g. `aitrainer://polar-callback`; verify Polar accepts it, otherwise propose an alternative). Exchange the code for a token, register the user with AccessLink once, then pull daily activity / calories daily. **Verify endpoints and flows against the official AccessLink v3 documentation** rather than assuming them.
- **Body weight from Polar**: read it from AccessLink's physical information data (weight is part of the user's physical settings in Polar Flow). Before building this, check in the docs how weight history is exposed (a new entry per change vs only the current value) and report back; if only the current value is available, store a new `body_weight` row whenever the value changes.
- Keep **manual weight entry** in the app as a fallback (source MANUAL).
- If both Polar and Health Connect provide burned calories, Polar wins; record the source.

## Screens (MVP)

1. **Log workout**: start a session (from the plan or ad hoc), add exercises, log sets quickly ("same as last time", +/- buttons), finish.
2. **Plan**: current week, today's session highlighted, daily kcal/protein target, "Adjust today" button.
3. **Progress**: weight chart (raw + 7-day average), daily/weekly calorie balance bars, weekly sets per muscle group.
4. **Settings**: goal and preferences, weak muscle groups, equipment, exercise preferences, Claude API key and model, Polar connect/disconnect, Health Connect permissions, DB export/import.
5. **Debug** (debug builds only): seed demo data, switch between real and fake Claude/Polar clients.

## Testing

- **Prefer integration tests that follow a user flow**: Compose UI tests in `androidTest/` that launch the real app (real Koin graph, real Room database) and tap through a screen the way I would, then check what's shown. Use fakes only at external boundaries (Claude, Polar, Health Connect), never for our own classes. Use the `androidx.compose.ui.test.junit4.v2` rules (the non-v2 ones are deprecated).
- UI tests run in **English only** (the test sets the app locale to `en`); don't repeat them per language. Translations are covered by lint's `MissingTranslation`.
- **Keep unit tests few and meaningful** (`./gradlew test`): only for non-trivial pure logic where a bug would be subtle (e.g. domain stats like TDEE and weight trend, plan JSON validation). No tests that only add coverage (trivial mappings, converters, getters, data lists). Don't mock our own code; use the real implementations.
- Run the relevant tests after changes. Instrumented tests (`./gradlew connectedDebugAndroidTest`) uninstall the app afterwards, which deletes its data, so run them on the emulator, not on my phone.
- **Expected values are literals, never computed.** Decide the expected outcome first (the exact text, numbers, order), write it into the test as a literal, then run the test. Tests contain only technical logic: setup, clicking/typing, waiting, reading what's shown, asserting. Don't build expected values from string resources, plural rules, formatters or the production code under test.
- **Test class naming**: integration tests end with `ITest` (e.g. `LogWorkoutFlowITest`), unit tests with `UTest` (e.g. `ExercisePickerUTest`).
- Write tests with **`kotlin.test`** (`kotlin.test.Test`, `assertEquals`, `@BeforeTest`, …), running on JUnit 4 via `kotlin-test-junit`, in both `test/` and `androidTest/`. Don't use JUnit 5. Only use `org.junit` for things `kotlin.test` lacks (`@RunWith`, `@get:Rule`).
- **Koin check**: a unit test that verifies the Koin module graph (`verify()` / `checkModules`), so missing dependencies fail in tests instead of crashing at runtime.
- **Build check** (`./gradlew assembleDebug`) after each meaningful change; fix compile errors before reporting done.
- **Fakes**: `FakeClaudeClient` (returns a canned valid plan), `FakePolarClient`, and a fake health data source, swappable via Koin modules so I can develop the UI on the emulator offline and without API costs.
- **Seed demo data** (debug-only button): generate ~6 weeks of realistic workouts, weight and calorie data to exercise the charts and plan generation.
- Add `@Preview` functions with fake state for the main screens.
- I test on an emulator (API 34+, Google Play image; Health Connect is built in) and on my own phone via USB or wireless debugging.

## Build phases

Finish each phase in a working, runnable state (builds, tests pass, app launches) before starting the next. At the end of each phase, summarize what was done and what I should check manually.

1. **Foundation**: add Koin, KSP, Room, Navigation, Ktor, serialization and kotlinx-datetime to the existing project; `Application` class starting Koin; entities, DAOs, seed exercise library; app shell with bottom navigation; workout logging screen; disable backups; Koin module verification test.
2. **Settings + secrets**: `SecretStore` (DataStore + Keystore AES-GCM, with a unit/instrumented test for round-trip encryption), settings screen, profile/preferences, debug screen with seed demo data.
3. **Health Connect + progress**: permissions, sync worker, manual weight entry, progress screen with charts.
4. **Polar**: OAuth flow, user registration, daily activity sync, body weight sync.
5. **Claude**: domain stats, prompt builder, client (real + fake), plan generation, plan screen, "adjust today".
6. **Polish**: weekly review worker, DB export/import, error states, empty states.

## Conventions

- Kotlin coding conventions; small composables; UI state as immutable data classes exposed via `StateFlow`.
- **No comments in code** (Kotlin, XML, Gradle, tests). Names and structure must explain the code: when something isn't clear, rename, extract a well-named function/constant/type, or restructure instead of commenting. Explain Android concepts and design reasons in your reply, not in code.
- Commit-sized changes; don't refactor unrelated code.
- Never commit secrets, keystores or `local.properties`.