package com.github.sebastianp265.aitrainer.integration.nutrition

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface NutritionService {
    fun observeTarget(): Flow<NutritionTarget?>

    suspend fun setTarget(kcal: Int?, proteinG: Int?, reason: String?)
}

data class NutritionTarget(val kcal: Int?, val proteinG: Int?, val setOn: LocalDate, val reason: String?)
