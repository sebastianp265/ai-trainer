package com.github.sebastianp265.aitrainer.feature.workout

import com.github.sebastianp265.aitrainer.feature.workout.data.RoomWorkoutRepository
import com.github.sebastianp265.aitrainer.feature.workout.domain.WorkoutRepository
import com.github.sebastianp265.aitrainer.feature.workout.domain.usecase.AddExerciseToWorkoutUseCase
import com.github.sebastianp265.aitrainer.feature.workout.domain.usecase.AddSetUseCase
import com.github.sebastianp265.aitrainer.feature.workout.domain.usecase.ChangeSetUseCase
import com.github.sebastianp265.aitrainer.feature.workout.domain.usecase.CopySetsFromLastTimeUseCase
import com.github.sebastianp265.aitrainer.feature.workout.ui.LogWorkoutViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val workoutModule = module {
    singleOf(::RoomWorkoutRepository) bind WorkoutRepository::class
    factoryOf(::AddExerciseToWorkoutUseCase)
    factoryOf(::AddSetUseCase)
    factoryOf(::CopySetsFromLastTimeUseCase)
    factoryOf(::ChangeSetUseCase)
    viewModelOf(::LogWorkoutViewModel)
}
