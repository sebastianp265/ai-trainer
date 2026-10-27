package com.github.sebastianp265.aitrainer.domain.stats

import com.github.sebastianp265.aitrainer.domain.model.WeightMeasurement
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

data class DailyWeight(val date: LocalDate, val weightKg: Double)

private const val MOVING_AVERAGE_DAYS = 7
private const val MIN_DAYS_SPANNED_FOR_TREND = 7
private const val DAYS_PER_WEEK = 7

fun dailyAverageWeights(measurements: List<WeightMeasurement>, timeZone: TimeZone): List<DailyWeight> =
    measurements.groupBy { it.measuredAt.toLocalDateTime(timeZone).date }
        .map { (date, sameDay) -> DailyWeight(date, sameDay.map { it.weightKg }.average()) }
        .sortedBy { it.date }

fun sevenDayMovingAverage(dailyWeights: List<DailyWeight>): List<DailyWeight> =
    dailyWeights.map { day ->
        val windowStart = day.date.minus(DatePeriod(days = MOVING_AVERAGE_DAYS - 1))
        val window = dailyWeights.filter { it.date in windowStart..day.date }
        DailyWeight(day.date, window.map { it.weightKg }.average())
    }

fun weeklyWeightChangeKg(dailyWeights: List<DailyWeight>): Double? {
    if (dailyWeights.size < 2) return null
    val firstDate = dailyWeights.first().date
    if (firstDate.daysUntil(dailyWeights.last().date) < MIN_DAYS_SPANNED_FOR_TREND) return null
    val points = dailyWeights.map { firstDate.daysUntil(it.date).toDouble() to it.weightKg }
    return leastSquaresSlope(points) * DAYS_PER_WEEK
}

private fun leastSquaresSlope(points: List<Pair<Double, Double>>): Double {
    val meanX = points.map { it.first }.average()
    val meanY = points.map { it.second }.average()
    val covariance = points.sumOf { (x, y) -> (x - meanX) * (y - meanY) }
    val varianceX = points.sumOf { (x, _) -> (x - meanX) * (x - meanX) }
    return covariance / varianceX
}
