package com.github.sebastianp265.aitrainer.shared.database.impl

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppDatabaseMigrationUTest {

    private companion object {
        val SIX_GROUPS = setOf("CHEST", "BACK", "LEGS", "SHOULDERS", "ARMS", "ABS")
    }

    private val helper = newHelper()

    @Test
    fun `migration from 6 to 7 adds empty health tables and keeps workouts`() {
        helper.createDatabase(6).use { connection ->
            connection.execSQL("INSERT INTO workout_session (id, date, startedAt, finishedAt, planSessionRef, notes) VALUES (1, '2026-11-01', 1000, 2000, NULL, NULL)")
        }

        helper.runMigrationsAndValidate(7).use { connection ->
            assertEquals(1, connection.count("workout_session"))
            assertEquals(0, connection.count("health_day"))
            assertEquals(0, connection.count("health_workout"))
        }
    }

    @Test
    fun `migration from 7 to 8 gives every seeded exercise one primary group among the six`() {
        val migrated = migratedSeedOfVersion7()

        val primaryCounts = migrated.primaryGroupsByExercise.mapValues { it.value.size }
        assertEquals(migrated.exercises, primaryCounts.keys)
        assertTrue(primaryCounts.values.all { it == 1 })
        assertEquals(SIX_GROUPS, migrated.groups)
    }

    @Test
    fun `migration from 7 to 8 leaves the same exercise muscles as a freshly seeded database`() {
        val migrated = migratedSeedOfVersion7()
        val fresh = newHelper().createDatabase(8).use { connection ->
            runSeed(connection, "/seed.sql")
            connection.snapshotOfMuscles()
        }

        assertEquals(fresh, migrated)
    }

    @Test
    fun `fresh seed gives every exercise one primary group`() {
        val fresh = newHelper().createDatabase(8).use { connection ->
            runSeed(connection, "/seed.sql")
            connection.snapshotOfMuscles()
        }

        assertTrue(fresh.exercises.isNotEmpty())
        assertTrue(fresh.primaryGroupsByExercise.values.all { it.size == 1 })
        assertEquals(SIX_GROUPS, fresh.groups)
    }

    @Test
    fun `migration from 8 to 9 keeps workouts without a plan and adds empty plan tables`() {
        helper.createDatabase(8).use { connection ->
            connection.execSQL("INSERT INTO workout_session (id, date, startedAt, finishedAt, planSessionRef, notes) VALUES (1, '2026-11-01', 1000, 2000, NULL, 'felt good')")
        }

        helper.runMigrationsAndValidate(9).use { connection ->
            assertEquals(1, connection.count("workout_session"))
            assertEquals(1, connection.count("workout_session WHERE planId IS NULL AND notes = 'felt good'"))
            assertEquals(0, connection.count("plan"))
            assertEquals(0, connection.count("planned_set"))
        }
    }

    private fun migratedSeedOfVersion7(): MuscleSnapshot {
        helper.createDatabase(7).use { runSeed(it, "/seed-v7.sql") }
        return helper.runMigrationsAndValidate(8, listOf(migration7To8)).use { it.snapshotOfMuscles() }
    }

    private fun runSeed(connection: SQLiteConnection, resource: String) {
        checkNotNull(javaClass.getResource(resource)).readText().lines().filter { it.isNotBlank() }
            .forEach(connection::execSQL)
    }

    private fun SQLiteConnection.snapshotOfMuscles(): MuscleSnapshot {
        val exercises = column("SELECT key FROM exercise").toSet()
        val primaries = column("SELECT exerciseKey || ':' || muscleGroupKey FROM exercise_muscle WHERE isPrimary = 1")
        val secondaries = column("SELECT exerciseKey || ':' || muscleGroupKey FROM exercise_muscle WHERE isPrimary = 0")
        return MuscleSnapshot(
            exercises = exercises,
            primaryGroupsByExercise = primaries.groupBy({ it.substringBefore(':') }, { it.substringAfter(':') }),
            secondaryGroups = secondaries.toSet(),
            groups = column("SELECT key FROM muscle_group").toSet(),
            translatedGroups = column("SELECT DISTINCT muscleGroupKey FROM muscle_group_translation").toSet(),
        )
    }

    private fun SQLiteConnection.column(sql: String): List<String> = prepare(sql).use { statement ->
        buildList { while (statement.step()) add(statement.getText(0)) }
    }

    private data class MuscleSnapshot(
        val exercises: Set<String>,
        val primaryGroupsByExercise: Map<String, List<String>>,
        val secondaryGroups: Set<String>,
        val groups: Set<String>,
        val translatedGroups: Set<String>,
    )

    private fun newHelper() = MigrationTestHelper(
        schemaDirectoryPath = Path.of("schemas"),
        databasePath = Files.createTempFile("migration", ".db"),
        driver = BundledSQLiteDriver(),
        databaseClass = AppDatabase::class,
        databaseFactory = { AppDatabaseConstructor.initialize() },
    )

    private fun SQLiteConnection.count(table: String): Int =
        prepare("SELECT COUNT(*) FROM $table").use { statement ->
            statement.step()
            statement.getLong(0).toInt()
        }
}
