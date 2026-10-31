package com.github.sebastianp265.aitrainer.feature.settings

import com.github.sebastianp265.aitrainer.core.DataStoreFiles
import com.github.sebastianp265.aitrainer.feature.settings.data.DataStoreAppSettingsRepository
import com.github.sebastianp265.aitrainer.feature.settings.domain.AppSettingsRepository
import com.github.sebastianp265.aitrainer.feature.settings.ui.SettingsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val settingsModule = module {
    single<AppSettingsRepository> { DataStoreAppSettingsRepository(get(named(DataStoreFiles.SETTINGS))) }
    viewModelOf(::SettingsViewModel)
}
