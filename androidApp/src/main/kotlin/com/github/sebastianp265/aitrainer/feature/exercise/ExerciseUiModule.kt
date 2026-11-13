package com.github.sebastianp265.aitrainer.feature.exercise

import com.github.sebastianp265.aitrainer.feature.exercise.impl.ExerciseHistoryViewModel
import com.github.sebastianp265.aitrainer.feature.exercise.impl.ExercisePickerViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val exerciseUiModule = module {
    viewModelOf(::ExercisePickerViewModel)
    viewModelOf(::ExerciseHistoryViewModel)
}
