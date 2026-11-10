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
            connection.usePrepared("INSERT INTO equipment (key) VALUES ('BARBELL')") { it.step() }
        }
    }
    return database
}

internal fun AppDatabase.seedExercises(vararg keys: String) = runBlocking {
    useWriterConnection { connection ->
        keys.forEach { connection.usePrepared("INSERT INTO exercise (key, equipmentKey) VALUES ('$it', 'BARBELL')") { statement -> statement.step() } }
    }
}
