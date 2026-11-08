package com.github.sebastianp265.aitrainer.shared.database

import androidx.room.RoomDatabase
import com.github.sebastianp265.aitrainer.shared.database.impl.AppDatabase
import com.github.sebastianp265.aitrainer.integration.shared.DatabaseBuilders
import org.koin.dsl.module
import org.koin.dsl.onClose

internal val databaseModule = module {
    single {
        get<DatabaseBuilders>().builder(AppDatabase::class, AppDatabase.NAME)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .apply { getAll<RoomDatabase.Callback>().forEach { addCallback(it) } }
            .build()
    } onClose { it?.close() }
    single { get<AppDatabase>().exerciseDao() }
    single { get<AppDatabase>().workoutDao() }
}
