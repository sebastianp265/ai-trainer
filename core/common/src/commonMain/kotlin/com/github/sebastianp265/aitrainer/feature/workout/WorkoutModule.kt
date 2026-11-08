package com.github.sebastianp265.aitrainer.feature.workout

import com.github.sebastianp265.aitrainer.feature.workout.impl.DefaultWorkoutService
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutService
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal val workoutModule = module {
    factoryOf(::DefaultWorkoutService) bind WorkoutService::class
}
