package com.github.sebastianp265.aitrainer.feature.health.impl

import com.github.sebastianp265.aitrainer.integration.health.DailyAmount
import com.github.sebastianp265.aitrainer.integration.health.ExternalWorkoutReading
import com.github.sebastianp265.aitrainer.integration.health.HealthConnectAvailability
import com.github.sebastianp265.aitrainer.integration.health.HealthDataSource
import com.github.sebastianp265.aitrainer.integration.health.HealthDataType
import com.github.sebastianp265.aitrainer.integration.health.NutritionTotals
import com.github.sebastianp265.aitrainer.integration.health.SleepSession
import com.github.sebastianp265.aitrainer.integration.health.TimedValue
import kotlinx.datetime.LocalDate

internal class FakeHealthDataSource : HealthDataSource {
    var granted: Set<HealthDataType> = HealthDataType.entries.toSet()
    var historyGranted = false
    var nutrition: List<NutritionTotals> = emptyList()
    var steps: List<DailyAmount> = emptyList()
    var workouts: List<ExternalWorkoutReading> = emptyList()
    val readFrom = mutableListOf<LocalDate>()

    override val permissionsToRequest: Set<String> = emptySet()
    override fun availability() = HealthConnectAvailability.AVAILABLE
    override suspend fun grantedTypes() = granted
    override suspend fun hasHistoryPermission() = historyGranted

    override suspend fun readNutrition(from: LocalDate, toInclusive: LocalDate): List<NutritionTotals> {
        readFrom += from
        return nutrition
    }

    override suspend fun readCaloriesBurned(from: LocalDate, toInclusive: LocalDate) = emptyList<DailyAmount>()
    override suspend fun readSteps(from: LocalDate, toInclusive: LocalDate) = steps
    override suspend fun readWeights(from: LocalDate, toInclusive: LocalDate) = emptyList<TimedValue>()
    override suspend fun readSleep(from: LocalDate, toInclusive: LocalDate) = emptyList<SleepSession>()
    override suspend fun readRestingHeartRates(from: LocalDate, toInclusive: LocalDate) = emptyList<TimedValue>()
    override suspend fun readHeartRateVariabilities(from: LocalDate, toInclusive: LocalDate) = emptyList<TimedValue>()
    override suspend fun readWorkouts(from: LocalDate, toInclusive: LocalDate) = workouts
}
