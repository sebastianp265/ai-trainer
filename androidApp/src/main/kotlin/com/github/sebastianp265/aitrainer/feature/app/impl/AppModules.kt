package com.github.sebastianp265.aitrainer.feature.app.impl

import com.github.sebastianp265.aitrainer.feature.exercise.exerciseUiModule
import com.github.sebastianp265.aitrainer.feature.health.healthUiModule
import com.github.sebastianp265.aitrainer.feature.plan.planUiModule
import com.github.sebastianp265.aitrainer.feature.history.historyUiModule
import com.github.sebastianp265.aitrainer.feature.progress.progressUiModule
import com.github.sebastianp265.aitrainer.feature.settings.settingsUiModule
import com.github.sebastianp265.aitrainer.feature.volume.volumeUiModule
import com.github.sebastianp265.aitrainer.feature.workout.workoutUiModule
import com.github.sebastianp265.aitrainer.integration.module.androidModules
import com.github.sebastianp265.aitrainer.integration.module.commonModules
import com.github.sebastianp265.aitrainer.feature.coach.coachUiModule
import com.github.sebastianp265.aitrainer.shared.coach.pendingProposalUiModule
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import kotlin.time.Clock

private val clockModule = module {
    single<Clock> { Clock.System }
}

private val shellUiModule = module {
    viewModelOf(::CoachEnabledViewModel)
}

internal val uiModules = listOf(
    shellUiModule,
    coachUiModule,
    pendingProposalUiModule,
    exerciseUiModule,
    planUiModule,
    volumeUiModule,
    workoutUiModule,
    healthUiModule,
    historyUiModule,
    progressUiModule,
    settingsUiModule,
)

internal val appModules = listOf(clockModule) + commonModules + androidModules + uiModules
