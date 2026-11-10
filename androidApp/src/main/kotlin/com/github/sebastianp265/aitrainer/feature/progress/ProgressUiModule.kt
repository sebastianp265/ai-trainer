package com.github.sebastianp265.aitrainer.feature.progress

import com.github.sebastianp265.aitrainer.feature.progress.impl.DayDetailViewModel
import com.github.sebastianp265.aitrainer.feature.progress.impl.MetricViewModel
import com.github.sebastianp265.aitrainer.feature.progress.impl.ProgressViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val progressUiModule = module {
    viewModelOf(::ProgressViewModel)
    viewModelOf(::MetricViewModel)
    viewModelOf(::DayDetailViewModel)
}
