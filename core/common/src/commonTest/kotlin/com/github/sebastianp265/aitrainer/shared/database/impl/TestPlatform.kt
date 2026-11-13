package com.github.sebastianp265.aitrainer.shared.database.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.testing.MigrationTestHelper
import androidx.room.useWriterConnection

internal expect fun emptyInMemoryDatabase(): AppDatabase

internal expect fun temporaryDataStore(name: String): DataStore<Preferences>

internal expect fun <T> blocking(block: suspend () -> T): T

internal expect fun newMigrationHelper(): MigrationTestHelper

internal expect fun readResource(path: String): String

internal fun inMemoryDatabase(): AppDatabase {
    val database = emptyInMemoryDatabase()
    blocking {
        database.useWriterConnection { connection ->
            listOf(
                "INSERT INTO equipment (key) VALUES ('BARBELL'), ('OTHER')",
                "INSERT INTO muscle_group (key) VALUES ('CHEST'), ('BACK'), ('LEGS'), ('SHOULDERS'), ('ARMS'), ('ABS')",
                "INSERT INTO muscle_group_translation (muscleGroupKey, language, name) VALUES ('CHEST', 'en', 'Chest'), ('BACK', 'en', 'Back'), ('LEGS', 'en', 'Legs'), ('SHOULDERS', 'en', 'Shoulders'), ('ARMS', 'en', 'Arms'), ('ABS', 'en', 'Abs')",
                "INSERT INTO equipment_translation (equipmentKey, language, name) VALUES ('BARBELL', 'en', 'Barbell'), ('OTHER', 'en', 'Other')",
            ).forEach { sql -> connection.usePrepared(sql) { it.step() } }
        }
    }
    return database
}

internal fun AppDatabase.seedExercises(vararg keys: String) = blocking {
    useWriterConnection { connection ->
        keys.forEach { connection.usePrepared("INSERT INTO exercise (key, equipmentKey) VALUES ('$it', 'BARBELL')") { statement -> statement.step() } }
    }
}

internal fun AppDatabase.seedExerciseMuscles(exerciseKey: String, primary: String, vararg secondary: String) = blocking {
    useWriterConnection { connection ->
        connection.usePrepared("INSERT INTO exercise_muscle (exerciseKey, muscleGroupKey, isPrimary) VALUES ('$exerciseKey', '$primary', 1)") { it.step() }
        secondary.forEach { group ->
            connection.usePrepared("INSERT INTO exercise_muscle (exerciseKey, muscleGroupKey, isPrimary) VALUES ('$exerciseKey', '$group', 0)") { it.step() }
        }
    }
}

internal fun AppDatabase.seedExerciseName(exerciseKey: String, name: String) = blocking {
    useWriterConnection { connection ->
        connection.usePrepared("INSERT INTO exercise_translation (exerciseKey, language, name) VALUES ('$exerciseKey', 'en', '$name')") { it.step() }
    }
}
