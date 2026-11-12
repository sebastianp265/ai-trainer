package com.github.sebastianp265.aitrainer.feature.health.impl

import com.github.sebastianp265.aitrainer.integration.health.DailyAmount
import com.github.sebastianp265.aitrainer.integration.health.HealthConnectAvailability
import com.github.sebastianp265.aitrainer.integration.health.HealthDataSource
import com.github.sebastianp265.aitrainer.integration.health.HealthDataType
import com.github.sebastianp265.aitrainer.integration.health.NutritionTotals
import com.github.sebastianp265.aitrainer.integration.health.SleepSession
import com.github.sebastianp265.aitrainer.integration.health.TimedValue
import kotlinx.datetime.LocalDate

internal class FakeHealthDataSource : HealthDataSource {
    var availability = HealthConnectAvailability.AVAILABLE
    var granted: Set<HealthDataType> = HealthDataType.entries.toSet()
    var historyGranted = false
    var nutrition: List<NutritionTotals> = emptyList()
    var steps: List<DailyAmount> = emptyList()

    override fun availability() = availability
    override suspend fun grantedTypes() = granted
    override suspend fun missingPermissions(type: HealthDataType) = if (type in granted) emptySet() else setOf("read:$type")
    override suspend fun missingHistoryPermissions() = if (historyGranted) emptySet() else setOf("read:history")
    override suspend fun readNutrition(from: LocalDate, toInclusive: LocalDate) = nutrition.filter { it.date in from..toInclusive }
    override suspend fun readCaloriesBurned(from: LocalDate, toInclusive: LocalDate) = emptyList<DailyAmount>()
    override suspend fun readSteps(from: LocalDate, toInclusive: LocalDate) = steps.filter { it.date in from..toInclusive }
    override suspend fun readWeights(from: LocalDate, toInclusive: LocalDate) = emptyList<TimedValue>()
    override suspend fun readSleep(from: LocalDate, toInclusive: LocalDate) = emptyList<SleepSession>()
    override suspend fun readRestingHeartRates(from: LocalDate, toInclusive: LocalDate) = emptyList<TimedValue>()
    override suspend fun readHeartRateVariabilities(from: LocalDate, toInclusive: LocalDate) = emptyList<TimedValue>()
}
