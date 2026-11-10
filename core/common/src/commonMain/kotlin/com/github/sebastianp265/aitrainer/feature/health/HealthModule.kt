package com.github.sebastianp265.aitrainer.feature.health

import com.github.sebastianp265.aitrainer.feature.health.impl.DefaultHealthService
import com.github.sebastianp265.aitrainer.integration.health.HealthService
import com.github.sebastianp265.aitrainer.integration.shared.PreferencesStores
import kotlinx.datetime.TimeZone
import org.koin.core.qualifier.named
import org.koin.dsl.module

private const val HEALTH_DATA_STORE_FILE = "health"

internal val healthModule = module {
    single(named(HEALTH_DATA_STORE_FILE)) { get<PreferencesStores>().create(HEALTH_DATA_STORE_FILE) }
    factory<HealthService> {
        DefaultHealthService(get(), get(), get(named(HEALTH_DATA_STORE_FILE)), get(), TimeZone.currentSystemDefault())
    }
}
