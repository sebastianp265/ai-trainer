package com.github.sebastianp265.aitrainer.shared.health

import com.github.sebastianp265.aitrainer.integration.health.HealthDataSource
import com.github.sebastianp265.aitrainer.shared.health.impl.HealthConnectDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

internal val healthDataSourceModule = module {
    single<HealthDataSource> { HealthConnectDataSource(androidContext()) }
}
