# :core:common

Kotlin Multiplatform library, JVM target only, no Android SDK. Code is in `commonMain`, tests are in `commonTest`; `jvmTest` holds only the JVM `actual`s of `TestPlatform` (the in-memory database, a temporary DataStore file, `runBlocking`, the migration test helper, reading a resource). The tests run with `:core:common:jvmTest`. Room uses **KSP** (`kspJvm`), not kapt.

## Persistence

- Room entities and DAOs live in `shared.<domain>`; `shared.database` assembles `AppDatabase` and its Koin module. Services map entities to `integration` models.
- Room schema export writes `core/common/schemas/<database class>/<version>.json` (the folder is named after `AppDatabase`'s fully qualified name, so moving that class creates a new folder: delete the old one). **Commit `core/common/schemas/`**.
- **Every schema change gets a migration**, so installing a new build never deletes data: bump `@Database(version = …)`, add an `AutoMigration` where Room can derive it, otherwise a manual `Migration`, and a migration test. Commit the new schema JSON; a migration starts from the previous one.
- Seed data changes go through migrations too: new exercises are inserted by the migration that adds them. `seed.sql` only fills a freshly created database.

## Data invariants

- `set_entry.positionInSession` is the set's position within the whole session, not within the exercise.
- Dates are stored as ISO text, instants as epoch millis, enum lists as comma-separated names (`shared/database/impl/TimeConverters.kt`).
- Exercises, muscle groups, equipment and their translations are Room tables (`exercise`, `muscle_group`, `equipment`, each with a `…_translation` table keyed by `(key, language)`; `exercise_muscle` links exercises to muscle groups). They are filled from `src/commonMain/resources/seed.sql` (one `INSERT` per line) by `ExerciseSeedCallback` when the database is created, never on app start, and never from code. `set_entry.exerciseKey` is a foreign key to `exercise.key`, the stable id used in code and later by Claude. `ExerciseSeedITest` (in `:core:android`) checks that everything is translated in every supported language.
- Health Connect data is never stored in Room. `DefaultHealthService` reads it from Health Connect when a screen or the coach needs it (daily values are computed by `HealthDayRules` on each read) and reads again after `reload()`, which the app calls when it comes to the foreground.
- Domain arithmetic (stats, TDEE, trends) is computed locally in the feature's `impl`, never by Claude.

## Coach and AI

- `CoachService` is only the chat: messages, activity, proposals (accept, undo), the weekly review. `CoachMemoryService` holds the profile and notes. `feature.coach` stores them and applies accepted proposals; it asks a `CoachResponder` for replies and hands it a `ProposalDesk` to propose changes. It knows nothing about what answers.
- `feature.ai` is the responder in use: the Claude tool loop (`AiCoachResponder`, `AiToolbox`, prompt), the API key, model and tuning (`AiService`), and the usage of the model (table `ai_usage`). Its tools reach the app only through `integration`.
