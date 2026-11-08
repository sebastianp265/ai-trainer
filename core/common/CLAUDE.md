# :core:common

Kotlin Multiplatform library, JVM target only, `commonMain` only, no Android SDK. Room uses **KSP** (`kspJvm`), not kapt.

## Persistence

- Room entities and DAOs live in `shared.<domain>`; `shared.database` assembles `AppDatabase` and its Koin module. Services map entities to `integration` models.
- Room schema export writes `core/common/schemas/<database class>/<version>.json` (the folder is named after `AppDatabase`'s fully qualified name, so moving that class creates a new folder: delete the old one). **Commit `core/common/schemas/`**.
- **Prototype: no migrations.** On a schema change, bump the database version (the DB is recreated). Details and the end-of-prototype steps: `docs/prototype-database.md`.

## Data invariants

- `set_entry.positionInSession` is the set's position within the whole session, not within the exercise.
- Dates are stored as ISO text, instants as epoch millis, enum lists as comma-separated names (`shared/database/impl/TimeConverters.kt`).
- Exercises, muscle groups, equipment and their translations are Room tables (`exercise`, `muscle_group`, `equipment`, each with a `…_translation` table keyed by `(key, language)`; `exercise_muscle` links exercises to muscle groups). They are filled from `src/commonMain/resources/seed.sql` (one `INSERT` per line) by `ExerciseSeedCallback` when the database is created or recreated, never on app start, and never from code. `set_entry.exerciseKey` is a foreign key to `exercise.key`, the stable id used in code and later by Claude. `ExerciseSeedITest` (in `:core:android`) checks that everything is translated in every supported language.
- Health Connect data is not stored in the app: it's read on demand each time it's shown.
- Domain arithmetic (stats, TDEE, trends) is computed locally in the feature's `impl`, never by Claude.
