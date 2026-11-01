package com.github.sebastianp265.aitrainer.feature.settings

import com.github.sebastianp265.aitrainer.feature.settings.impl.DefaultSettingsService
import com.github.sebastianp265.aitrainer.integration.shared.PreferencesStores
import com.github.sebastianp265.aitrainer.integration.settings.SettingsService
import org.koin.core.qualifier.named
import org.koin.dsl.module

private const val SETTINGS_DATA_STORE_FILE = "settings"

internal val settingsModule = module {
    single(named(SETTINGS_DATA_STORE_FILE)) { get<PreferencesStores>().create(SETTINGS_DATA_STORE_FILE) }
    factory<SettingsService> { DefaultSettingsService(get(named(SETTINGS_DATA_STORE_FILE)), get()) }
}
