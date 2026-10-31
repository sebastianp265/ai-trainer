package com.github.sebastianp265.aitrainer.feature.progress

import com.github.sebastianp265.aitrainer.feature.progress.ui.ProgressViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val progressModule = module {
    viewModelOf(::ProgressViewModel)
}
