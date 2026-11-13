package com.github.sebastianp265.aitrainer.feature.coach

import com.github.sebastianp265.aitrainer.feature.coach.impl.DefaultCoachMemoryService
import com.github.sebastianp265.aitrainer.feature.coach.impl.DefaultCoachService
import com.github.sebastianp265.aitrainer.feature.coach.impl.DefaultProposalDesk
import com.github.sebastianp265.aitrainer.integration.coach.CoachMemoryService
import com.github.sebastianp265.aitrainer.integration.coach.CoachService
import com.github.sebastianp265.aitrainer.integration.coach.ProposalDesk
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal val coachModule = module {
    factoryOf(::DefaultProposalDesk) bind ProposalDesk::class
    factoryOf(::DefaultCoachMemoryService) bind CoachMemoryService::class
    single<CoachService> {
        DefaultCoachService(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get())
    }
}
