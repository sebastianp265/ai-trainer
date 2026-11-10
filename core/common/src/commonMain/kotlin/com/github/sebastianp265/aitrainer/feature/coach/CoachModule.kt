package com.github.sebastianp265.aitrainer.feature.coach

import com.github.sebastianp265.aitrainer.feature.coach.impl.CoachToolbox
import com.github.sebastianp265.aitrainer.feature.coach.impl.DefaultCoachService
import com.github.sebastianp265.aitrainer.feature.coach.impl.KtorClaudeClient
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeClient
import com.github.sebastianp265.aitrainer.integration.coach.CoachService
import com.github.sebastianp265.aitrainer.integration.shared.REAL_CLAUDE_CLIENT
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal val coachModule = module {
    factory<ClaudeClient>(named(REAL_CLAUDE_CLIENT)) { KtorClaudeClient(get(), get()) }
    factoryOf(::CoachToolbox)
    single<CoachService> {
        DefaultCoachService({ get() }, get(), get(), get(), get(), get(), get(), get(), get())
    }
}
