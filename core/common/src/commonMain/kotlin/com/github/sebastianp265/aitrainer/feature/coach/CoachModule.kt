package com.github.sebastianp265.aitrainer.feature.coach

import com.github.sebastianp265.aitrainer.feature.coach.impl.CoachToolbox
import com.github.sebastianp265.aitrainer.feature.coach.impl.DefaultCoachService
import com.github.sebastianp265.aitrainer.integration.coach.CoachService
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

internal val coachModule = module {
    factoryOf(::CoachToolbox)
    single<CoachService> {
        DefaultCoachService({ get() }, get(), get(), get(), get(), get(), get(), get(), get(), get(), get())
    }
}
