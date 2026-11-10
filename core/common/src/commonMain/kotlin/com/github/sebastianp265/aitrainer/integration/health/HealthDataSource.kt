package com.github.sebastianp265.aitrainer.integration.health

import kotlinx.datetime.LocalDate

enum class HealthConnectAvailability { AVAILABLE, UPDATE_REQUIRED, UNAVAILABLE }

interface HealthDataSource {
    val permissionsToRequest: Set<String>

    fun availability(): HealthConnectAvailability

    suspend fun grantedTypes(): Set<HealthDataType>

    suspend fun hasHistoryPermission(): Boolean

    suspend fun readNutrition(from: LocalDate, toInclusive: LocalDate): List<NutritionTotals>

    suspend fun readCaloriesBurned(from: LocalDate, toInclusive: LocalDate): List<DailyAmount>

    suspend fun readSteps(from: LocalDate, toInclusive: LocalDate): List<DailyAmount>

    suspend fun readWeights(from: LocalDate, toInclusive: LocalDate): List<TimedValue>

    suspend fun readSleep(from: LocalDate, toInclusive: LocalDate): List<SleepSession>

    suspend fun readRestingHeartRates(from: LocalDate, toInclusive: LocalDate): List<TimedValue>

    suspend fun readHeartRateVariabilities(from: LocalDate, toInclusive: LocalDate): List<TimedValue>

    suspend fun readWorkouts(from: LocalDate, toInclusive: LocalDate): List<ExternalWorkoutReading>
}
