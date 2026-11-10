package com.github.sebastianp265.aitrainer.feature.health

import com.github.sebastianp265.aitrainer.feature.health.impl.HealthConnectViewModel
import com.github.sebastianp265.aitrainer.feature.health.impl.HealthSyncViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val healthUiModule = module {
    viewModelOf(::HealthConnectViewModel)
    viewModelOf(::HealthSyncViewModel)
}
