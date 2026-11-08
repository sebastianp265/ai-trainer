package com.github.sebastianp265.aitrainer.feature.workout

import com.github.sebastianp265.aitrainer.feature.workout.impl.LogWorkoutViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val workoutUiModule = module {
    viewModelOf(::LogWorkoutViewModel)
}
