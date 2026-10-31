package com.github.sebastianp265.aitrainer.feature.health

import com.github.sebastianp265.aitrainer.feature.health.data.HealthConnectDataSource
import com.github.sebastianp265.aitrainer.feature.health.data.HealthDataSource
import com.github.sebastianp265.aitrainer.feature.health.domain.usecase.CheckHealthConnectAccessUseCase
import com.github.sebastianp265.aitrainer.feature.health.domain.usecase.ReadHealthConnectDaysUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val healthModule = module {
    single<HealthDataSource> { HealthConnectDataSource(androidContext()) }
    factoryOf(::CheckHealthConnectAccessUseCase)
    factoryOf(::ReadHealthConnectDaysUseCase)
}
