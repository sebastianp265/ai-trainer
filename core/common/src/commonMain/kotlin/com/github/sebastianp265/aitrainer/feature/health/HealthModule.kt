package com.github.sebastianp265.aitrainer.feature.health

import com.github.sebastianp265.aitrainer.feature.health.impl.DefaultHealthService
import com.github.sebastianp265.aitrainer.integration.health.HealthService
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal val healthModule = module {
    factoryOf(::DefaultHealthService) bind HealthService::class
}
