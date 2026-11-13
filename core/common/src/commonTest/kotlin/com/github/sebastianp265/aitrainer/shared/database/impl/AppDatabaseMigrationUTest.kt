package com.github.sebastianp265.aitrainer.shared.database.impl

import com.github.sebastianp265.aitrainer.integration.shared.DATABASE_SCHEMA_VERSION
import com.github.sebastianp265.aitrainer.integration.shared.OLDEST_MIGRATED_SCHEMA_VERSION
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppDatabaseMigrationUTest {

    private companion object {
        val SIX_GROUPS = setOf("CHEST", "BACK", "LEGS", "SHOULDERS", "ARMS", "ABS")
    }

    private val helper = newMigrationHelper()

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
    fun `migration from 7 to 8 leaves the same exercise muscles as a freshly seeded database`() {
        val migrated = migratedSeedOfVersion7()
        val fresh = newMigrationHelper().createDatabase(8).use { connection ->
            runSeed(connection, "/seed.sql")
            connection.snapshotOfMuscles()
        }

        assertEquals(fresh, migrated)
    }

    @Test
    fun `fresh seed gives every exercise one primary group`() {
        val fresh = newMigrationHelper().createDatabase(8).use { connection ->
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

    @Test
    fun `migration from 9 to 10 adds an empty weekly target table and keeps plans`() {
        helper.createDatabase(9).use { connection ->
            connection.execSQL("INSERT INTO plan (id, name, date, author, createdAt) VALUES (1, 'Push', '2026-11-12', 'COACH', 1000)")
        }

        helper.runMigrationsAndValidate(10).use { connection ->
            assertEquals(1, connection.count("plan"))
            assertEquals(0, connection.count("weekly_target"))
        }
    }

    @Test
    fun `migration from 10 to 11 adds empty coach tables and keeps targets`() {
        helper.createDatabase(10).use { connection ->
            connection.execSQL("INSERT INTO muscle_group (key) VALUES ('ABS')")
            connection.execSQL("INSERT INTO weekly_target (muscleGroupKey, minSets, maxSets, source, setOn, reason) VALUES ('ABS', 6, 10, 'USER', '2026-10-20', NULL)")
        }

        helper.runMigrationsAndValidate(11).use { connection ->
            assertEquals(1, connection.count("weekly_target"))
            listOf("coach_message", "coach_proposal", "coach_note", "coach_profile", "coach_usage", "coach_log").forEach {
                assertEquals(0, connection.count(it))
            }
        }
    }

    @Test
    fun `migration from 11 to 12 adds an empty review column and keeps messages`() {
        helper.createDatabase(11).use { connection ->
            connection.execSQL("INSERT INTO coach_message (role, text, createdAt, checked) VALUES ('USER', 'Hi', 1000, NULL)")
        }

        helper.runMigrationsAndValidate(12).use { connection ->
            assertEquals(1, connection.count("coach_message"))
            assertEquals(1, connection.count("coach_message WHERE review IS NULL"))
        }
    }

    @Test
    fun `migration from 12 to 13 removes the request log and keeps messages`() {
        helper.createDatabase(12).use { connection ->
            connection.execSQL("INSERT INTO coach_message (role, text, createdAt, checked, review) VALUES ('USER', 'Hi', 1000, NULL, NULL)")
            connection.execSQL("INSERT INTO coach_log (at, kind, summary, details) VALUES (1000, 'REQUEST', 'request 1 messages', 'system')")
        }

        helper.runMigrationsAndValidate(13).use { connection ->
            assertEquals(1, connection.count("coach_message"))
            assertEquals(0, connection.count("sqlite_master WHERE name = 'coach_log'"))
        }
    }

    @Test
    fun `migration from 13 to 14 adds an empty coach note to plans`() {
        helper.createDatabase(13).use { connection ->
            connection.execSQL("INSERT INTO plan (id, name, date, author, createdAt) VALUES (1, 'Push', '2026-11-12', 'COACH', 1000)")
        }

        helper.runMigrationsAndValidate(14).use { connection ->
            assertEquals(1, connection.count("plan WHERE coachNote IS NULL"))
        }
    }

    @Test
    fun `migration from 14 to 15 gives proposals no undo yet`() {
        helper.createDatabase(14).use { connection ->
            connection.execSQL("INSERT INTO coach_proposal (id, type, state, summary, payload, subjectKey, messageId, createdAt) VALUES (1, 'NEW_PLAN', 'ACCEPTED', 's', '{}', NULL, NULL, 1000)")
        }

        helper.runMigrationsAndValidate(15).use { connection ->
            assertEquals(1, connection.count("coach_proposal WHERE undo IS NULL"))
        }
    }

    @Test
    fun `migration from 15 to 16 drops the copied Health Connect data and topics about workouts from other apps`() {
        helper.createDatabase(15).use { connection ->
            connection.execSQL("INSERT INTO workout_session (id, date, startedAt, finishedAt, planId, notes) VALUES (1, '2026-11-01', 1000, 2000, NULL, NULL)")
            connection.execSQL("INSERT INTO health_day (date) VALUES ('2026-11-01')")
            connection.execSQL("INSERT INTO coach_message (id, role, text, createdAt, topic_kind, topic_id, topic_label) VALUES (1, 'USER', 'a', 1000, 'EXTERNAL_WORKOUT', 'x', 'Run'), (2, 'USER', 'b', 2000, 'PLAN', '1', 'Push A')")
        }

        helper.runMigrationsAndValidate(16).use { connection ->
            assertEquals(1, connection.count("workout_session"))
            assertEquals(0, connection.count("sqlite_master WHERE name IN ('health_day', 'health_workout')"))
            assertEquals(1, connection.count("coach_message WHERE topic_kind IS NULL AND topic_id IS NULL AND topic_label IS NULL"))
            assertEquals(1, connection.count("coach_message WHERE topic_kind = 'PLAN'"))
        }
    }

    @Test
    fun `migration from 16 to 17 keeps workouts and marks none of them as an edit`() {
        helper.createDatabase(16).use { connection ->
            connection.execSQL("INSERT INTO workout_session (id, date, startedAt, finishedAt, planId, notes) VALUES (1, '2026-11-01', 1000, 2000, NULL, NULL)")
        }

        helper.runMigrationsAndValidate(17).use { connection ->
            assertEquals(1, connection.count("workout_session WHERE editedSessionId IS NULL"))
        }
    }

    @Test
    fun `migration from 17 to 18 drops the plan date and keeps plans with their sets`() {
        helper.createDatabase(17).use { connection ->
            connection.execSQL("INSERT INTO plan (id, name, date, author, createdAt, coachNote) VALUES (1, 'Push A', '2026-11-12', 'COACH', 1000, NULL)")
            connection.execSQL("INSERT INTO equipment (key) VALUES ('BARBELL')")
            connection.execSQL("INSERT INTO exercise (key, equipmentKey) VALUES ('BENCH', 'BARBELL')")
            connection.execSQL("INSERT INTO planned_set (id, planId, exerciseKey, positionInPlan, reps, weightKg, rpe, isWarmup) VALUES (1, 1, 'BENCH', 0, 8, 80.0, NULL, 0)")
        }

        helper.runMigrationsAndValidate(18).use { connection ->
            assertEquals(1, connection.count("plan WHERE name = 'Push A'"))
            assertEquals(1, connection.count("planned_set WHERE planId = 1"))
        }
    }

    @Test
    fun `migration from 18 to 19 keeps the order in which plans were made`() {
        helper.createDatabase(18).use { connection ->
            connection.execSQL("INSERT INTO plan (id, name, author, createdAt, coachNote) VALUES (3, 'Third', 'USER', 1000, NULL), (5, 'Fifth', 'COACH', 2000, NULL)")
        }

        helper.runMigrationsAndValidate(19).use { connection ->
            assertEquals(listOf("Third", "Fifth"), connection.column("SELECT name FROM plan ORDER BY position"))
        }
    }

    @Test
    fun `migration from 19 to 20 keeps the usage of the model under its new table name`() {
        helper.createDatabase(19).use { connection ->
            connection.execSQL("INSERT INTO coach_usage (month, messages, inputTokens, outputTokens) VALUES ('2026-11', 3, 1200, 340)")
        }

        helper.runMigrationsAndValidate(20).use { connection ->
            assertEquals(1, connection.count("ai_usage WHERE month = '2026-11' AND messages = 3 AND inputTokens = 1200 AND outputTokens = 340"))
            assertEquals(0, connection.count("sqlite_master WHERE name = 'coach_usage'"))
        }
    }

    @Test
    fun `a backup of schema 12 migrates to the current schema and keeps workouts, plans and the coach`() {
        helper.createDatabase(12).use { connection ->
            connection.execSQL("INSERT INTO workout_session (id, date, startedAt, finishedAt, planId, notes) VALUES (1, '2026-11-01', 1000, 2000, NULL, 'felt good')")
            connection.execSQL("INSERT INTO plan (id, name, date, author, createdAt) VALUES (1, 'Push A', '2026-11-12', 'COACH', 1000)")
            connection.execSQL("INSERT INTO coach_message (id, role, text, createdAt, checked, review) VALUES (1, 'USER', 'Plan my week', 1000, NULL, NULL)")
            connection.execSQL("INSERT INTO coach_note (id, text, createdAt) VALUES (1, 'Knee hurts on squats', 1000)")
        }

        helper.runMigrationsAndValidate(DATABASE_SCHEMA_VERSION).use { connection ->
            assertEquals(1, connection.count("workout_session WHERE notes = 'felt good'"))
            assertEquals(1, connection.count("plan WHERE name = 'Push A'"))
            assertEquals(1, connection.count("coach_message WHERE text = 'Plan my week'"))
            assertEquals(1, connection.count("coach_note WHERE text = 'Knee hurts on squats'"))
        }
    }

    @Test
    fun `the oldest migrated schema reaches the current schema and keeps workouts`() {
        helper.createDatabase(OLDEST_MIGRATED_SCHEMA_VERSION).use { connection ->
            connection.execSQL("INSERT INTO workout_session (id, date, startedAt, finishedAt, planSessionRef, notes) VALUES (1, '2026-11-01', 1000, 2000, NULL, NULL)")
        }

        helper.runMigrationsAndValidate(DATABASE_SCHEMA_VERSION, listOf(migration7To8)).use { connection ->
            assertEquals(1, connection.count("workout_session"))
        }
    }

    private fun migratedSeedOfVersion7(): MuscleSnapshot {
        helper.createDatabase(7).use { runSeed(it, "/seed-v7.sql") }
        return helper.runMigrationsAndValidate(8, listOf(migration7To8)).use { it.snapshotOfMuscles() }
    }

    private fun runSeed(connection: SQLiteConnection, resource: String) {
        readResource(resource).lines().filter { it.isNotBlank() }.forEach(connection::execSQL)
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

    private fun SQLiteConnection.count(table: String): Int =
        prepare("SELECT COUNT(*) FROM $table").use { statement ->
            statement.step()
            statement.getLong(0).toInt()
        }
}
