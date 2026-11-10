package com.github.sebastianp265.aitrainer.feature.plan

import com.github.sebastianp265.aitrainer.feature.plan.impl.DefaultPlanService
import com.github.sebastianp265.aitrainer.integration.plan.PlanService
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

internal val planModule = module {
    factoryOf(::DefaultPlanService) { bind<PlanService>() }
}
