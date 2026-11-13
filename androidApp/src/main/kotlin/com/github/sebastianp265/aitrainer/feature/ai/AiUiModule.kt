package com.github.sebastianp265.aitrainer.feature.ai

import com.github.sebastianp265.aitrainer.feature.ai.impl.AiSettingsViewModel
import com.github.sebastianp265.aitrainer.feature.ai.impl.ApiKeyViewModel
import com.github.sebastianp265.aitrainer.feature.ai.impl.ModelPickerViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val aiUiModule = module {
    viewModelOf(::AiSettingsViewModel)
    viewModelOf(::ApiKeyViewModel)
    viewModelOf(::ModelPickerViewModel)
}
