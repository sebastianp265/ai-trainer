package com.github.sebastianp265.aitrainer.feature.nutrition

import com.github.sebastianp265.aitrainer.feature.nutrition.impl.DefaultNutritionService
import com.github.sebastianp265.aitrainer.integration.nutrition.NutritionService
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

internal val nutritionModule = module {
    factoryOf(::DefaultNutritionService) { bind<NutritionService>() }
}
