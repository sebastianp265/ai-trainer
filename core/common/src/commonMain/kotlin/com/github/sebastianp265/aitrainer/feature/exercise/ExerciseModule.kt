package com.github.sebastianp265.aitrainer.feature.exercise

import androidx.room.RoomDatabase
import com.github.sebastianp265.aitrainer.feature.exercise.impl.DefaultExerciseService
import com.github.sebastianp265.aitrainer.feature.exercise.impl.ExerciseSeedCallback
import com.github.sebastianp265.aitrainer.integration.exercise.ExerciseService
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

internal val exerciseModule = module {
    singleOf(::ExerciseSeedCallback) { bind<RoomDatabase.Callback>() }
    factoryOf(::DefaultExerciseService) { bind<ExerciseService>() }
}
