package com.github.sebastianp265.aitrainer.feature.settings

import com.github.sebastianp265.aitrainer.feature.settings.impl.SettingsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val settingsUiModule = module {
    viewModelOf(::SettingsViewModel)
}
