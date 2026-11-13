package com.github.sebastianp265.aitrainer.feature.nutrition.impl

import com.github.sebastianp265.aitrainer.integration.nutrition.NutritionService
import com.github.sebastianp265.aitrainer.integration.nutrition.NutritionTarget
import com.github.sebastianp265.aitrainer.shared.nutrition.NutritionDao
import com.github.sebastianp265.aitrainer.shared.nutrition.NutritionTargetEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

internal class DefaultNutritionService(
    private val dao: NutritionDao,
    private val clock: Clock,
) : NutritionService {

    override fun observeTarget(): Flow<NutritionTarget?> =
        dao.observeTarget().map { it?.let { target -> NutritionTarget(target.kcal, target.proteinG, target.setOn, target.reason) } }

    override suspend fun setTarget(kcal: Int?, proteinG: Int?, reason: String?) {
        if (kcal == null && proteinG == null) {
            dao.deleteTarget()
        } else {
            dao.upsertTarget(NutritionTargetEntity(kcal = kcal, proteinG = proteinG, setOn = clock.todayIn(TimeZone.currentSystemDefault()), reason = reason))
        }
    }
}
