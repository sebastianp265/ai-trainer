package com.github.sebastianp265.aitrainer.shared.database.impl

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

class AppDatabaseMigrationUTest {

    private val databasePath = Files.createTempFile("migration", ".db")
    private val helper = MigrationTestHelper(
        schemaDirectoryPath = Path.of("schemas"),
        databasePath = databasePath,
        driver = BundledSQLiteDriver(),
        databaseClass = AppDatabase::class,
        databaseFactory = { AppDatabaseConstructor.initialize() },
    )

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

    private fun SQLiteConnection.count(table: String): Int =
        prepare("SELECT COUNT(*) FROM $table").use { statement ->
            statement.step()
            statement.getLong(0).toInt()
        }
}
