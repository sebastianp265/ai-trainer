package com.github.sebastianp265.aitrainer.feature.debug.impl

import com.github.sebastianp265.aitrainer.integration.coach.ClaudeClient
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeContent
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeFailureKind
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeRequest
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeResponse
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeResult
import com.github.sebastianp265.aitrainer.integration.health.DailyAmount
import com.github.sebastianp265.aitrainer.integration.health.ExternalWorkoutReading
import com.github.sebastianp265.aitrainer.integration.health.HealthConnectAvailability
import com.github.sebastianp265.aitrainer.integration.health.HealthDataSource
import com.github.sebastianp265.aitrainer.integration.health.HealthDataType
import com.github.sebastianp265.aitrainer.integration.health.NutritionTotals
import com.github.sebastianp265.aitrainer.integration.health.SleepSession
import com.github.sebastianp265.aitrainer.integration.health.TimedValue
import com.github.sebastianp265.aitrainer.integration.settings.SettingsService
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

internal class SwitchingClaudeClient(
    private val real: ClaudeClient,
    private val canned: ClaudeClient,
    private val settings: SettingsService,
) : ClaudeClient {

    override suspend fun send(request: ClaudeRequest): ClaudeResult = pick().send(request)

    override suspend fun checkApiKey(apiKey: String): ClaudeFailureKind? = pick().checkApiKey(apiKey)

    private suspend fun pick(): ClaudeClient = if (settings.observeUseFakeClaude().first()) canned else real
}

internal class CannedClaudeClient : ClaudeClient {

    override suspend fun send(request: ClaudeRequest): ClaudeResult =
        ClaudeResult.Success(ClaudeResponse(listOf(ClaudeContent.Text("Debug answer: the fake coach is on.")), "end_turn", inputTokens = 1, outputTokens = 1))

    override suspend fun checkApiKey(apiKey: String): ClaudeFailureKind? = null
}

internal class SwitchingHealthDataSource(
    private val real: HealthDataSource,
    private val canned: HealthDataSource,
    private val settings: SettingsService,
) : HealthDataSource {

    override val permissionsToRequest: Set<String> get() = real.permissionsToRequest

    override fun availability(): HealthConnectAvailability = real.availability()

    override suspend fun grantedTypes(): Set<HealthDataType> = pick().grantedTypes()

    override suspend fun hasHistoryPermission(): Boolean = pick().hasHistoryPermission()

    override suspend fun readNutrition(from: LocalDate, toInclusive: LocalDate) = pick().readNutrition(from, toInclusive)

    override suspend fun readCaloriesBurned(from: LocalDate, toInclusive: LocalDate) = pick().readCaloriesBurned(from, toInclusive)

    override suspend fun readSteps(from: LocalDate, toInclusive: LocalDate) = pick().readSteps(from, toInclusive)

    override suspend fun readWeights(from: LocalDate, toInclusive: LocalDate) = pick().readWeights(from, toInclusive)

    override suspend fun readSleep(from: LocalDate, toInclusive: LocalDate) = pick().readSleep(from, toInclusive)

    override suspend fun readRestingHeartRates(from: LocalDate, toInclusive: LocalDate) = pick().readRestingHeartRates(from, toInclusive)

    override suspend fun readHeartRateVariabilities(from: LocalDate, toInclusive: LocalDate) = pick().readHeartRateVariabilities(from, toInclusive)

    override suspend fun readWorkouts(from: LocalDate, toInclusive: LocalDate) = pick().readWorkouts(from, toInclusive)

    private suspend fun pick(): HealthDataSource = if (settings.observeUseFakeHealth().first()) canned else real
}

internal class CannedHealthDataSource(private val clock: Clock) : HealthDataSource {

    override val permissionsToRequest: Set<String> = emptySet()

    override fun availability() = HealthConnectAvailability.AVAILABLE

    override suspend fun grantedTypes(): Set<HealthDataType> = HealthDataType.entries.toSet()

    override suspend fun hasHistoryPermission() = true

    override suspend fun readNutrition(from: LocalDate, toInclusive: LocalDate): List<NutritionTotals> =
        days(from, toInclusive).map { NutritionTotals(it, 2300, 150.0, 250.0, 40.0, 70.0, 20.0, 25.0, 2000.0) }

    override suspend fun readCaloriesBurned(from: LocalDate, toInclusive: LocalDate) = days(from, toInclusive).map { DailyAmount(it, 2600) }

    override suspend fun readSteps(from: LocalDate, toInclusive: LocalDate) = days(from, toInclusive).map { DailyAmount(it, 8500) }

    override suspend fun readWeights(from: LocalDate, toInclusive: LocalDate) =
        days(from, toInclusive).mapIndexed { index, date -> TimedValue(date.atStartOfDayIn(TimeZone.UTC), 82.0 - index * 0.05) }

    override suspend fun readSleep(from: LocalDate, toInclusive: LocalDate) =
        days(from, toInclusive).map { SleepSession(it.atStartOfDayIn(TimeZone.UTC), 450) }

    override suspend fun readRestingHeartRates(from: LocalDate, toInclusive: LocalDate) =
        days(from, toInclusive).map { TimedValue(it.atStartOfDayIn(TimeZone.UTC), 54.0) }

    override suspend fun readHeartRateVariabilities(from: LocalDate, toInclusive: LocalDate) =
        days(from, toInclusive).map { TimedValue(it.atStartOfDayIn(TimeZone.UTC), 62.0) }

    override suspend fun readWorkouts(from: LocalDate, toInclusive: LocalDate): List<ExternalWorkoutReading> = emptyList()

    private fun days(from: LocalDate, toInclusive: LocalDate): List<LocalDate> {
        val today = clock.todayIn(TimeZone.UTC)
        val last = minOf(toInclusive, today)
        val first = maxOf(from, last.minus(DatePeriod(days = MAX_DAYS)))
        return generateSequence(first) { it.plus(DatePeriod(days = 1)) }.takeWhile { it <= last }.toList()
    }

    private companion object {
        const val MAX_DAYS = 30
    }
}
