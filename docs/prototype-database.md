# Prototype database (no migrations)

While the app is a prototype, the Room database has **no migrations**. The goal is frictionless testing on the phone: no migration work for each schema change, and no crashes.

## How it works

1. SQLite stores a version number in the database file. Room compares it with `@Database(version = …)` when the database is first opened.
2. On a schema change `version` in `@Database` (`shared/database/impl/AppDatabase.kt`) is bumped. If the versions differ and no migration exists, `fallbackToDestructiveMigration(dropAllTables = true)` (database builder in `shared/database/DatabaseModule.kt`) drops every table and recreates them from the current entities. This works for upgrades and downgrades.
3. `ExerciseSeedCallback` (`feature/exercise/impl/ExerciseSeedCallback.kt`) runs `seed.sql` (exercises, muscle groups, equipment and translations) after the database is created or recreated.

What this means on the phone:

- Installing a new build without a schema change keeps all data.
- Installing a build with a schema change deletes all local data (workouts) once.

If you change the schema but forget to bump the version, the app crashes with *"Room cannot verify the data integrity"*. Room stores a fingerprint of the schema (identity hash) in `room_master_table`, and the destructive fallback only applies when the version changes.

## When the app is finished

Once all phases are done, switch to real migrations:

1. Set `@Database(version = 1)`.
2. Delete the old files in `core/common/schemas/` and build, so that `1.json` is the only exported schema. Commit it.
3. Remove `fallbackToDestructiveMigration(...)` from the database builder.
4. From then on, every schema change gets a migration (`AutoMigration` where possible) and a migration test.
5. Make the seed data update itself through migrations: new exercises get inserted by the migration that adds them.
6. Delete this file and its pointer in `CLAUDE.md`.
