package com.github.sebastianp265.aitrainer.feature.plan

import com.github.sebastianp265.aitrainer.feature.plan.impl.PlanDetailViewModel
import com.github.sebastianp265.aitrainer.feature.plan.impl.PlanEditViewModel
import com.github.sebastianp265.aitrainer.feature.plan.impl.TrainingWeekViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val planUiModule = module {
    viewModelOf(::TrainingWeekViewModel)
    viewModelOf(::PlanDetailViewModel)
    viewModelOf(::PlanEditViewModel)
}
