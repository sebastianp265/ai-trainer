package com.github.sebastianp265.aitrainer.shared.e2e

import com.github.sebastianp265.aitrainer.integration.health.DailyAmount
import com.github.sebastianp265.aitrainer.integration.health.ExternalWorkoutReading
import com.github.sebastianp265.aitrainer.integration.health.HealthConnectAvailability
import com.github.sebastianp265.aitrainer.integration.health.HealthDataSource
import com.github.sebastianp265.aitrainer.integration.health.HealthDataType
import com.github.sebastianp265.aitrainer.integration.health.NutritionTotals
import com.github.sebastianp265.aitrainer.integration.health.SleepSession
import com.github.sebastianp265.aitrainer.integration.health.TimedValue
import kotlinx.datetime.LocalDate

class FakeHealthService(
    var availability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
    var granted: Set<HealthDataType> = HealthDataType.entries.toSet(),
    var historyGranted: Boolean = true,
) : HealthDataSource {
    var nutrition: List<NutritionTotals> = emptyList()
    var caloriesBurned: List<DailyAmount> = emptyList()
    var steps: List<DailyAmount> = emptyList()
    var weights: List<TimedValue> = emptyList()
    var sleep: List<SleepSession> = emptyList()
    var restingHeartRates: List<TimedValue> = emptyList()
    var heartRateVariabilities: List<TimedValue> = emptyList()
    var workouts: List<ExternalWorkoutReading> = emptyList()

    override val permissionsToRequest: Set<String> = emptySet()
    override fun availability() = availability
    override suspend fun grantedTypes() = granted
    override suspend fun hasHistoryPermission() = historyGranted
    override suspend fun readNutrition(from: LocalDate, toInclusive: LocalDate) = nutrition
    override suspend fun readCaloriesBurned(from: LocalDate, toInclusive: LocalDate) = caloriesBurned
    override suspend fun readSteps(from: LocalDate, toInclusive: LocalDate) = steps
    override suspend fun readWeights(from: LocalDate, toInclusive: LocalDate) = weights
    override suspend fun readSleep(from: LocalDate, toInclusive: LocalDate) = sleep
    override suspend fun readRestingHeartRates(from: LocalDate, toInclusive: LocalDate) = restingHeartRates
    override suspend fun readHeartRateVariabilities(from: LocalDate, toInclusive: LocalDate) = heartRateVariabilities
    override suspend fun readWorkouts(from: LocalDate, toInclusive: LocalDate) = workouts
}
