package com.github.sebastianp265.aitrainer.feature.ai

import com.github.sebastianp265.aitrainer.feature.ai.impl.AiCoachResponder
import com.github.sebastianp265.aitrainer.feature.ai.impl.AiToolbox
import com.github.sebastianp265.aitrainer.feature.ai.impl.AiUsageLog
import com.github.sebastianp265.aitrainer.feature.ai.impl.DefaultAiService
import com.github.sebastianp265.aitrainer.integration.ai.AiClient
import com.github.sebastianp265.aitrainer.integration.ai.AiService
import com.github.sebastianp265.aitrainer.integration.coach.CoachResponder
import com.github.sebastianp265.aitrainer.integration.shared.PreferencesStores
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

private const val AI_DATA_STORE_FILE = "ai"

internal val aiModule = module {
    single(named(AI_DATA_STORE_FILE)) { get<PreferencesStores>().create(AI_DATA_STORE_FILE) }
    single { AiUsageLog(get(), get()) }
    single<AiService> { DefaultAiService(get(named(AI_DATA_STORE_FILE)), get(), { get<AiClient>() }, get(), get()) }
    factoryOf(::AiToolbox)
    single<CoachResponder> { AiCoachResponder({ get<AiClient>() }, get(), get(), get(), get(), get(), get(), get(), get(), get()) }
}
