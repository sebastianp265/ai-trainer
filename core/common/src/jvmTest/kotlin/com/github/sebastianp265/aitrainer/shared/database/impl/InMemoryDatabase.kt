package com.github.sebastianp265.aitrainer.shared.database.impl

import androidx.room.Room
import androidx.room.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

internal fun inMemoryDatabase(): AppDatabase {
    val database = Room.inMemoryDatabaseBuilder<AppDatabase>(factory = { AppDatabaseConstructor.initialize() })
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Unconfined)
        .build()
    runBlocking {
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

internal fun AppDatabase.seedExercises(vararg keys: String) = runBlocking {
    useWriterConnection { connection ->
        keys.forEach { connection.usePrepared("INSERT INTO exercise (key, equipmentKey) VALUES ('$it', 'BARBELL')") { statement -> statement.step() } }
    }
}

internal fun AppDatabase.seedExerciseMuscles(exerciseKey: String, primary: String, vararg secondary: String) = runBlocking {
    useWriterConnection { connection ->
        connection.usePrepared("INSERT INTO exercise_muscle (exerciseKey, muscleGroupKey, isPrimary) VALUES ('$exerciseKey', '$primary', 1)") { it.step() }
        secondary.forEach { group ->
            connection.usePrepared("INSERT INTO exercise_muscle (exerciseKey, muscleGroupKey, isPrimary) VALUES ('$exerciseKey', '$group', 0)") { it.step() }
        }
    }
}

internal fun AppDatabase.seedExerciseName(exerciseKey: String, name: String) = runBlocking {
    useWriterConnection { connection ->
        connection.usePrepared("INSERT INTO exercise_translation (exerciseKey, language, name) VALUES ('$exerciseKey', 'en', '$name')") { it.step() }
    }
}
