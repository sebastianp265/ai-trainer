package com.github.sebastianp265.aitrainer.domain.stats

import com.github.sebastianp265.aitrainer.domain.model.DailyEnergy
import com.github.sebastianp265.aitrainer.domain.model.EnergySource
import com.github.sebastianp265.aitrainer.domain.model.HealthDayTotals
import kotlinx.datetime.LocalDate

fun applyHealthConnectDay(date: LocalDate, existing: DailyEnergy?, health: HealthDayTotals?): DailyEnergy? {
    val burnFromOtherSource = existing?.takeIf { it.kcalBurnedSource != EnergySource.HEALTH_CONNECT }?.kcalBurned
    val healthConnectBurn = health?.kcalBurned
    val day = DailyEnergy(
        date = date,
        kcalBurned = burnFromOtherSource ?: healthConnectBurn,
        kcalBurnedSource = when {
            burnFromOtherSource != null -> existing.kcalBurnedSource
            healthConnectBurn != null -> EnergySource.HEALTH_CONNECT
            else -> null
        },
        kcalEaten = health?.kcalEaten,
        proteinG = health?.proteinG,
        carbsG = health?.carbsG,
        fatG = health?.fatG,
    )
    return day.takeUnless { it.hasNoValues() }
}

private fun DailyEnergy.hasNoValues() =
    kcalBurned == null && kcalEaten == null && proteinG == null && carbsG == null && fatG == null
