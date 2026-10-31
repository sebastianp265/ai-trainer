package com.github.sebastianp265.aitrainer.feature.exercise

import com.github.sebastianp265.aitrainer.feature.exercise.data.RoomExerciseRepository
import com.github.sebastianp265.aitrainer.feature.exercise.domain.ExerciseRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val exerciseModule = module {
    singleOf(::RoomExerciseRepository) bind ExerciseRepository::class
}
