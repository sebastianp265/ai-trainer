package com.github.sebastianp265.aitrainer.integration.shared

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.RoomDatabase
import kotlin.reflect.KClass

interface DatabaseBuilders {
    fun <T : RoomDatabase> builder(database: KClass<T>, name: String): RoomDatabase.Builder<T>
}

interface PreferencesStores {
    fun create(name: String): DataStore<Preferences>
}

interface BundledFiles {
    fun read(path: String): String
}

interface DatabaseBackup {
    suspend fun export(destination: String): Boolean

    suspend fun inspect(source: String): BackupContents?

    suspend fun import(source: String): Boolean
}

data class BackupContents(
    val schemaVersion: Int,
    val workouts: Int,
    val plans: Int,
    val coachNotes: Int,
    val coachMessages: Int,
)
