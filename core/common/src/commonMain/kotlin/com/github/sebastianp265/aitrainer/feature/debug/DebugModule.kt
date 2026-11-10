package com.github.sebastianp265.aitrainer.feature.debug

import com.github.sebastianp265.aitrainer.feature.debug.impl.CannedClaudeClient
import com.github.sebastianp265.aitrainer.feature.debug.impl.CannedHealthDataSource
import com.github.sebastianp265.aitrainer.feature.debug.impl.DefaultDebugService
import com.github.sebastianp265.aitrainer.feature.debug.impl.SwitchingClaudeClient
import com.github.sebastianp265.aitrainer.feature.debug.impl.SwitchingHealthDataSource
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeClient
import com.github.sebastianp265.aitrainer.integration.debug.DebugService
import com.github.sebastianp265.aitrainer.integration.health.HealthDataSource
import com.github.sebastianp265.aitrainer.integration.shared.REAL_CLAUDE_CLIENT
import com.github.sebastianp265.aitrainer.integration.shared.REAL_HEALTH_DATA_SOURCE
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal val debugModule = module {
    factoryOf(::DefaultDebugService) { bind<DebugService>() }
    factory<ClaudeClient> { SwitchingClaudeClient(get(named(REAL_CLAUDE_CLIENT)), CannedClaudeClient(), get()) }
    single<HealthDataSource> { SwitchingHealthDataSource(get(named(REAL_HEALTH_DATA_SOURCE)), CannedHealthDataSource(get()), get()) }
}
