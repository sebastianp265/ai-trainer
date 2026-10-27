package com.github.sebastianp265.aitrainer.domain.stats

import com.github.sebastianp265.aitrainer.domain.model.DailyEnergy
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.math.roundToInt

data class EnergyAverages(
    val averageIntakeKcal: Int?,
    val averageBurnKcal: Int?,
    val averageBalanceKcal: Int?,
)

data class WeeklyBalance(val weekStart: LocalDate, val balanceKcal: Int)

fun DailyEnergy.balanceKcal(): Int? {
    val eaten = kcalEaten ?: return null
    val burned = kcalBurned ?: return null
    return eaten - burned
}

fun energyAverages(days: List<DailyEnergy>): EnergyAverages = EnergyAverages(
    averageIntakeKcal = days.mapNotNull { it.kcalEaten }.roundedAverage(),
    averageBurnKcal = days.mapNotNull { it.kcalBurned }.roundedAverage(),
    averageBalanceKcal = days.mapNotNull { it.balanceKcal() }.roundedAverage(),
)

fun weeklyBalances(days: List<DailyEnergy>): List<WeeklyBalance> =
    days.mapNotNull { day -> day.balanceKcal()?.let { day.date.mondayOfWeek() to it } }
        .groupBy({ it.first }, { it.second })
        .map { (weekStart, balances) -> WeeklyBalance(weekStart, balances.sum()) }
        .sortedBy { it.weekStart }

fun LocalDate.mondayOfWeek(): LocalDate = minus(DatePeriod(days = dayOfWeek.ordinal - DayOfWeek.MONDAY.ordinal))

private fun List<Int>.roundedAverage(): Int? = if (isEmpty()) null else average().roundToInt()
