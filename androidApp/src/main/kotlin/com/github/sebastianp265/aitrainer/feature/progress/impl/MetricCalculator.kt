package com.github.sebastianp265.aitrainer.feature.progress.impl

import com.github.sebastianp265.aitrainer.feature.progress.ProgressMetric
import com.github.sebastianp265.aitrainer.integration.health.HealthDay
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus

internal data class MetricDetail(
    val average: Double,
    val change: Double?,
    val points: List<DailyPoint>,
    val averageSeries: List<DailyPoint>,
    val lowest: Double,
    val highest: Double,
    val changePerWeek: Double?,
    val newestFirst: List<DailyPoint>,
)

internal object MetricCalculator {

    private const val DAYS_PER_WEEK = 7.0

    fun calculate(storedDays: List<HealthDay>, metric: ProgressMetric, range: ProgressRange, today: LocalDate): MetricDetail? {
        val firstDay = today.minus(DatePeriod(days = range.days - 1))
        val days = storedDays.filter { it.date in firstDay..today }.sortedBy { it.date }
        val usable = if (metric.needsFinishedDay) days.filter { it.date < today } else days
        val trend = ProgressCalculator.trend(usable) { metric.dailyValue(it) } ?: return null
        val values = trend.points.map { it.value }
        val spanDays = trend.points.first().date.daysUntil(trend.points.last().date)
        return MetricDetail(
            average = trend.current,
            change = trend.change,
            points = trend.points,
            averageSeries = trend.average,
            lowest = values.min(),
            highest = values.max(),
            changePerWeek = trend.change?.takeIf { spanDays >= DAYS_PER_WEEK }?.let { it / (spanDays / DAYS_PER_WEEK) },
            newestFirst = trend.points.sortedByDescending { it.date },
        )
    }

    private val ProgressMetric.needsFinishedDay: Boolean
        get() = this == ProgressMetric.CALORIE_BALANCE || this == ProgressMetric.STEPS

    private fun ProgressMetric.dailyValue(day: HealthDay): Double? = when (this) {
        ProgressMetric.WEIGHT -> day.weightKg
        ProgressMetric.CALORIE_BALANCE ->
            if (day.kcalEaten != null && day.kcalBurned != null) (day.kcalEaten!! - day.kcalBurned!!).toDouble() else null
        ProgressMetric.STEPS -> day.steps?.toDouble()
        ProgressMetric.SLEEP -> day.sleepMinutes?.toDouble()
        ProgressMetric.RESTING_HEART_RATE -> day.restingHeartRate
        ProgressMetric.HEART_RATE_VARIABILITY -> day.hrvMs
    }
}
