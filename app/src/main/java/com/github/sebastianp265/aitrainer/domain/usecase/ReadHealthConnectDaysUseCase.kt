package com.github.sebastianp265.aitrainer.domain.usecase

import com.github.sebastianp265.aitrainer.data.healthconnect.HealthConnectDayDto
import com.github.sebastianp265.aitrainer.data.healthconnect.HealthDataSource
import com.github.sebastianp265.aitrainer.domain.model.HealthDay
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class ReadHealthConnectDaysUseCase(
    private val healthDataSource: HealthDataSource,
    private val clock: Clock,
) {
    private companion object {
        const val DAYS = 30
    }

    suspend operator fun invoke(): List<HealthDay> {
        val today = clock.todayIn(TimeZone.currentSystemDefault())
        val firstDay = today.minus(DatePeriod(days = DAYS - 1))
        return healthDataSource.readDailyTotals(firstDay, today)
            .filter { it.hasAnyValue() }
            .sortedByDescending { it.date }
            .map { HealthDay(it.date, it.kcalEaten, it.proteinG, it.carbsG, it.fatG, it.sourcePackages) }
    }

    private fun HealthConnectDayDto.hasAnyValue() =
        listOf(kcalEaten, proteinG, carbsG, fatG).any { it != null }
}
