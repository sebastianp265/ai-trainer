package com.github.sebastianp265.aitrainer.shared.database.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.RoomDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.Dispatchers

internal expect fun inMemoryDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>

internal expect fun temporaryDataStore(name: String): DataStore<Preferences>

internal expect fun newMigrationHelper(): MigrationTestHelper

internal expect fun readResource(path: String): String

private val baseRows = listOf(
    "INSERT INTO equipment (key) VALUES ('BARBELL'), ('OTHER')",
    "INSERT INTO muscle_group (key) VALUES ('CHEST'), ('BACK'), ('LEGS'), ('SHOULDERS'), ('ARMS'), ('ABS')",
    "INSERT INTO muscle_group_translation (muscleGroupKey, language, name) VALUES ('CHEST', 'en', 'Chest'), ('BACK', 'en', 'Back'), ('LEGS', 'en', 'Legs'), ('SHOULDERS', 'en', 'Shoulders'), ('ARMS', 'en', 'Arms'), ('ABS', 'en', 'Abs')",
    "INSERT INTO equipment_translation (equipmentKey, language, name) VALUES ('BARBELL', 'en', 'Barbell'), ('OTHER', 'en', 'Other')",
)

internal fun inMemoryDatabase(vararg seeds: List<String>): AppDatabase =
    inMemoryDatabaseBuilder()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Unconfined)
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(connection: SQLiteConnection) = (baseRows + seeds.flatMap { it }).forEach(connection::execSQL)
        })
        .build()

internal fun seedExercises(vararg keys: String) =
    keys.map { "INSERT INTO exercise (key, equipmentKey) VALUES ('$it', 'BARBELL')" }

internal fun seedExerciseMuscles(exerciseKey: String, primary: String, vararg secondary: String) =
    listOf("INSERT INTO exercise_muscle (exerciseKey, muscleGroupKey, isPrimary) VALUES ('$exerciseKey', '$primary', 1)") +
        secondary.map { "INSERT INTO exercise_muscle (exerciseKey, muscleGroupKey, isPrimary) VALUES ('$exerciseKey', '$it', 0)" }

internal fun seedExerciseName(exerciseKey: String, name: String) =
    listOf("INSERT INTO exercise_translation (exerciseKey, language, name) VALUES ('$exerciseKey', 'en', '$name')")
