package com.github.sebastianp265.aitrainer.feature.exercise.impl

import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import com.github.sebastianp265.aitrainer.integration.shared.BundledFiles

internal class ExerciseSeedCallback(private val bundledFiles: BundledFiles) : RoomDatabase.Callback() {

    override fun onCreate(connection: SQLiteConnection) = runSeed(connection)

    private fun runSeed(connection: SQLiteConnection) {
        bundledFiles.read(SEED_FILE).lines().filter { it.isNotBlank() }.forEach { statement ->
            connection.prepare(statement).use { it.step() }
        }
    }

    private companion object {
        const val SEED_FILE = "seed.sql"
    }
}
