package com.github.sebastianp265.aitrainer.feature.health

import com.github.sebastianp265.aitrainer.feature.health.impl.HealthConnectViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val healthUiModule = module {
    viewModelOf(::HealthConnectViewModel)
}
