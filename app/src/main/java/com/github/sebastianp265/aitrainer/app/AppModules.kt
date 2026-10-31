package com.github.sebastianp265.aitrainer.app

import androidx.room.Room
import com.github.sebastianp265.aitrainer.core.coreModule
import com.github.sebastianp265.aitrainer.feature.exercise.data.ExerciseLibraryCallback
import com.github.sebastianp265.aitrainer.feature.exercise.exerciseModule
import com.github.sebastianp265.aitrainer.feature.health.healthModule
import com.github.sebastianp265.aitrainer.feature.progress.progressModule
import com.github.sebastianp265.aitrainer.feature.settings.settingsModule
import com.github.sebastianp265.aitrainer.feature.workout.workoutModule
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, AppDatabase.NAME)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .addCallback(ExerciseLibraryCallback())
            .build()
    }
    single { get<AppDatabase>().exerciseDao() }
    single { get<AppDatabase>().workoutDao() }
}

val featureModules = listOf(
    exerciseModule,
    workoutModule,
    healthModule,
    progressModule,
    settingsModule,
)

val appModules = listOf(coreModule, databaseModule) + featureModules
