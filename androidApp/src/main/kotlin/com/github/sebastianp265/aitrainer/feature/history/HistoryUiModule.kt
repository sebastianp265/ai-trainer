package com.github.sebastianp265.aitrainer.feature.history

import com.github.sebastianp265.aitrainer.feature.history.impl.ExternalWorkoutViewModel
import com.github.sebastianp265.aitrainer.feature.history.impl.HistoryViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val historyUiModule = module {
    viewModelOf(::HistoryViewModel)
    viewModelOf(::ExternalWorkoutViewModel)
}
