package com.github.sebastianp265.aitrainer.shared.database.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.nio.file.Files
import java.nio.file.Path

internal actual fun inMemoryDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> =
    Room.inMemoryDatabaseBuilder<AppDatabase>(factory = { AppDatabaseConstructor.initialize() })

internal actual fun temporaryDataStore(name: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(produceFile = { Files.createTempDirectory(name).resolve("$name.preferences_pb").toFile() })

internal actual fun newMigrationHelper() = MigrationTestHelper(
    schemaDirectoryPath = Path.of(checkNotNull(System.getProperty("roomSchemaDirectory"))),
    databasePath = Files.createTempFile("migration", ".db"),
    driver = BundledSQLiteDriver(),
    databaseClass = AppDatabase::class,
    databaseFactory = { AppDatabaseConstructor.initialize() },
)

internal actual fun readResource(path: String): String = checkNotNull(AppDatabase::class.java.getResource(path)).readText()
