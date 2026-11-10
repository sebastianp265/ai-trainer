package com.github.sebastianp265.aitrainer.feature.progress.impl

import com.github.sebastianp265.aitrainer.shared.charts.DailyPoint
import com.github.sebastianp265.aitrainer.integration.health.HealthDay
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.math.roundToInt

internal enum class ProgressRange(val days: Int) {
    FOUR_WEEKS(28),
    THREE_MONTHS(91),
    YEAR(365),
}


internal data class TrendCard(
    val current: Double,
    val change: Double?,
    val points: List<DailyPoint>,
    val average: List<DailyPoint>,
)

internal data class WeekBar(val weekStart: LocalDate, val balanceKcal: Int)

internal data class BalanceCard(
    val averageBalanceKcal: Int,
    val averageEatenKcal: Int,
    val averageBurnedKcal: Int,
    val weeks: List<WeekBar>,
)

internal data class MacrosCard(
    val proteinG: Double?,
    val carbsG: Double?,
    val fatG: Double?,
    val fiberG: Double?,
)

internal data class WorkoutsCard(val count: Int, val perWeek: Double)

internal data class ProgressCards(
    val workouts: WorkoutsCard?,
    val weight: TrendCard?,
    val balance: BalanceCard?,
    val macros: MacrosCard?,
    val steps: Double?,
    val sleepMinutes: Double?,
    val restingHeartRate: TrendCard?,
    val hrvMs: Double?,
    val recentDays: List<HealthDay>,
) {
    val isEmpty: Boolean
        get() = workouts == null && weight == null && balance == null && macros == null && steps == null && sleepMinutes == null &&
            restingHeartRate == null && hrvMs == null && recentDays.isEmpty()
}

internal object ProgressCalculator {

    private const val AVERAGE_WINDOW_DAYS = 7
    private const val RECENT_DAYS = 7
    private const val DAYS_PER_WEEK = 7.0

    fun calculate(
        storedDays: List<HealthDay>,
        finishedWorkouts: Int,
        range: ProgressRange,
        today: LocalDate,
    ): ProgressCards {
        val firstDay = today.minus(DatePeriod(days = range.days - 1))
        val days = storedDays.filter { it.date in firstDay..today }.sortedBy { it.date }
        val finishedDays = days.filter { it.date < today }
        return ProgressCards(
            workouts = finishedWorkouts.takeIf { it > 0 }?.let { WorkoutsCard(it, it * DAYS_PER_WEEK / range.days) },
            weight = trend(days) { it.weightKg },
            balance = balance(finishedDays),
            macros = macros(finishedDays),
            steps = finishedDays.mapNotNull { it.steps }.averageOrNull(),
            sleepMinutes = days.mapNotNull { it.sleepMinutes }.averageOrNull(),
            restingHeartRate = trend(days) { it.restingHeartRate },
            hrvMs = days.mapNotNull { it.hrvMs }.averageOrNull(),
            recentDays = days.sortedByDescending { it.date }.take(RECENT_DAYS),
        )
    }

    fun trend(days: List<HealthDay>, value: (HealthDay) -> Double?): TrendCard? {
        val points = days.mapNotNull { day -> value(day)?.let { DailyPoint(day.date, it) } }
        if (points.isEmpty()) return null
        val average = points.map { point ->
            val window = points.filter { it.date <= point.date && it.date > point.date.minus(DatePeriod(days = AVERAGE_WINDOW_DAYS)) }
            DailyPoint(point.date, window.map { it.value }.average())
        }
        val change = if (average.size >= 2) average.last().value - average.first().value else null
        return TrendCard(average.last().value, change, points, average)
    }

    private fun balance(days: List<HealthDay>): BalanceCard? {
        val withBoth = days.filter { it.kcalEaten != null && it.kcalBurned != null }
        if (withBoth.isEmpty()) return null
        val balances = withBoth.map { it.date to (it.kcalEaten!! - it.kcalBurned!!) }
        val weeks = balances.groupBy { (date, _) -> date.minus(DatePeriod(days = date.dayOfWeek.ordinal)) }
            .map { (weekStart, ofWeek) -> WeekBar(weekStart, ofWeek.map { it.second }.average().roundToInt()) }
            .sortedBy { it.weekStart }
        return BalanceCard(
            averageBalanceKcal = balances.map { it.second }.average().roundToInt(),
            averageEatenKcal = withBoth.map { it.kcalEaten!! }.average().roundToInt(),
            averageBurnedKcal = withBoth.map { it.kcalBurned!! }.average().roundToInt(),
            weeks = weeks,
        )
    }

    private fun macros(days: List<HealthDay>): MacrosCard? {
        val card = MacrosCard(
            proteinG = days.mapNotNull { it.proteinG }.averageOrNull(),
            carbsG = days.mapNotNull { it.carbsG }.averageOrNull(),
            fatG = days.mapNotNull { it.fatG }.averageOrNull(),
            fiberG = days.mapNotNull { it.fiberG }.averageOrNull(),
        )
        return card.takeIf { listOf(it.proteinG, it.carbsG, it.fatG, it.fiberG).any { value -> value != null } }
    }

    private fun List<Number>.averageOrNull(): Double? = takeIf { it.isNotEmpty() }?.map { it.toDouble() }?.average()
}
