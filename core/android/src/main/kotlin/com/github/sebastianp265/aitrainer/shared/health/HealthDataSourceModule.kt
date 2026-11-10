package com.github.sebastianp265.aitrainer.shared.health

import com.github.sebastianp265.aitrainer.integration.health.HealthDataSource
import com.github.sebastianp265.aitrainer.shared.health.impl.HealthConnectDataSource
import com.github.sebastianp265.aitrainer.integration.shared.REAL_HEALTH_DATA_SOURCE
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal val healthDataSourceModule = module {
    single<HealthDataSource>(named(REAL_HEALTH_DATA_SOURCE)) { HealthConnectDataSource(androidContext()) }
}
