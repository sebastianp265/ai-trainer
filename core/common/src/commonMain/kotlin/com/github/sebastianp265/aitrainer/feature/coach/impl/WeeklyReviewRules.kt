package com.github.sebastianp265.aitrainer.feature.coach.impl

import com.github.sebastianp265.aitrainer.integration.coach.WeeklyReviewSummary
import com.github.sebastianp265.aitrainer.integration.health.HealthDay
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.serialization.Serializable

internal fun mondayOf(date: LocalDate): LocalDate = date.minus(DatePeriod(days = date.dayOfWeek.ordinal))

internal fun weekBeforeToday(today: LocalDate): LocalDate = mondayOf(today).minus(DatePeriod(days = DAYS_IN_WEEK))

internal fun reviewSummary(
    weekStart: LocalDate,
    workouts: Int,
    records: Int,
    thisWeek: List<HealthDay>,
    weekBefore: List<HealthDay>,
): WeeklyReviewSummary {
    val weights = thisWeek.sortedBy { it.date }.mapNotNull { it.weightKg }
    val balanceDays = thisWeek.filter { it.kcalEaten != null && it.kcalBurned != null }
    val sleep = thisWeek.mapNotNull { it.sleepMinutes }
    val sleepBefore = weekBefore.mapNotNull { it.sleepMinutes }
    val proteins = thisWeek.mapNotNull { it.proteinG }
    return WeeklyReviewSummary(
        weekStart = weekStart,
        weekEnd = weekStart.plus(DatePeriod(days = DAYS_IN_WEEK - 1)),
        workouts = workouts,
        records = records,
        weightFromKg = weights.firstOrNull(),
        weightToKg = weights.lastOrNull()?.takeIf { weights.size > 1 },
        averageBalanceKcal = balanceDays.takeIf { it.isNotEmpty() }?.map { it.kcalEaten!! - it.kcalBurned!! }?.average()?.toInt(),
        averageProteinG = proteins.takeIf { it.isNotEmpty() }?.average()?.toInt(),
        averageSleepMinutes = sleep.takeIf { it.isNotEmpty() }?.average()?.toInt(),
        sleepChangeMinutes = if (sleep.isNotEmpty() && sleepBefore.isNotEmpty()) (sleep.average() - sleepBefore.average()).toInt() else null,
    )
}

internal fun WeeklyReviewSummary.hasAnything(): Boolean =
    workouts > 0 || weightFromKg != null || averageBalanceKcal != null || averageSleepMinutes != null

@Serializable
internal data class ReviewPayload(
    val weekStart: String,
    val weekEnd: String,
    val workouts: Int,
    val records: Int,
    val weightFromKg: Double? = null,
    val weightToKg: Double? = null,
    val averageBalanceKcal: Int? = null,
    val averageProteinG: Int? = null,
    val averageSleepMinutes: Int? = null,
    val sleepChangeMinutes: Int? = null,
)

internal fun WeeklyReviewSummary.toPayload() = ReviewPayload(
    weekStart.toString(), weekEnd.toString(), workouts, records,
    weightFromKg, weightToKg, averageBalanceKcal, averageProteinG, averageSleepMinutes, sleepChangeMinutes,
)

internal fun ReviewPayload.toSummary() = WeeklyReviewSummary(
    LocalDate.parse(weekStart), LocalDate.parse(weekEnd), workouts, records,
    weightFromKg, weightToKg, averageBalanceKcal, averageProteinG, averageSleepMinutes, sleepChangeMinutes,
)

private const val DAYS_IN_WEEK = 7
