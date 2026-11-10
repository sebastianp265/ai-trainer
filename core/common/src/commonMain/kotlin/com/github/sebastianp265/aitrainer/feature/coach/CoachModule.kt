package com.github.sebastianp265.aitrainer.feature.coach

import com.github.sebastianp265.aitrainer.feature.coach.impl.CoachToolbox
import com.github.sebastianp265.aitrainer.feature.coach.impl.DefaultCoachService
import com.github.sebastianp265.aitrainer.feature.coach.impl.KtorClaudeClient
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeClient
import com.github.sebastianp265.aitrainer.integration.coach.CoachService
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

internal val coachModule = module {
    factoryOf(::KtorClaudeClient) { bind<ClaudeClient>() }
    factoryOf(::CoachToolbox)
    singleOf(::DefaultCoachService) { bind<CoachService>() }
}
