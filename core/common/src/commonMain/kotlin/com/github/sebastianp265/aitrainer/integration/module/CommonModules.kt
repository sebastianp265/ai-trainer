package com.github.sebastianp265.aitrainer.integration.module

import com.github.sebastianp265.aitrainer.feature.exercise.exerciseModule
import com.github.sebastianp265.aitrainer.feature.coach.coachModule
import com.github.sebastianp265.aitrainer.feature.health.healthModule
import com.github.sebastianp265.aitrainer.feature.plan.planModule
import com.github.sebastianp265.aitrainer.feature.settings.settingsModule
import com.github.sebastianp265.aitrainer.feature.volume.volumeModule
import com.github.sebastianp265.aitrainer.feature.workout.workoutModule
import com.github.sebastianp265.aitrainer.shared.database.databaseModule

val commonModules = listOf(
    databaseModule,
    exerciseModule,
    workoutModule,
    planModule,
    volumeModule,
    coachModule,
    healthModule,
    settingsModule,
)

