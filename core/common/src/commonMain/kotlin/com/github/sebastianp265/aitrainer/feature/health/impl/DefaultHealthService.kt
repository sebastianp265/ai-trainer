package com.github.sebastianp265.aitrainer.feature.health.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.github.sebastianp265.aitrainer.integration.health.HealthConnectAccess
import com.github.sebastianp265.aitrainer.integration.health.HealthConnectAvailability
import com.github.sebastianp265.aitrainer.integration.health.HealthDataSource
import com.github.sebastianp265.aitrainer.integration.health.HealthDataType
import com.github.sebastianp265.aitrainer.integration.health.HealthDataType.CALORIES_BURNED
import com.github.sebastianp265.aitrainer.integration.health.HealthDataType.HEART_RATE_VARIABILITY
import com.github.sebastianp265.aitrainer.integration.health.HealthDataType.NUTRITION
import com.github.sebastianp265.aitrainer.integration.health.HealthDataType.RESTING_HEART_RATE
import com.github.sebastianp265.aitrainer.integration.health.HealthDataType.SLEEP
import com.github.sebastianp265.aitrainer.integration.health.HealthDataType.STEPS
import com.github.sebastianp265.aitrainer.integration.health.HealthDataType.WEIGHT
import com.github.sebastianp265.aitrainer.integration.health.HealthDataType.WORKOUTS
import com.github.sebastianp265.aitrainer.integration.health.HealthDay
import com.github.sebastianp265.aitrainer.integration.health.HealthService
import com.github.sebastianp265.aitrainer.integration.health.HealthStatus
import com.github.sebastianp265.aitrainer.integration.health.HealthWorkout
import com.github.sebastianp265.aitrainer.integration.health.NutritionTotals
import com.github.sebastianp265.aitrainer.shared.health.HealthDao
import com.github.sebastianp265.aitrainer.shared.health.HealthDayEntity
import com.github.sebastianp265.aitrainer.shared.health.HealthWorkoutEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Instant

internal class DefaultHealthService(
    private val dataSource: HealthDataSource,
    private val dao: HealthDao,
    private val preferences: DataStore<Preferences>,
    private val clock: Clock,
    private val zone: TimeZone,
) : HealthService {

    private val lastSyncKey = longPreferencesKey("last_sync_epoch_millis")

    override val permissionsToRequest: Set<String> get() = dataSource.permissionsToRequest

    override suspend fun status(): HealthStatus = when (dataSource.availability()) {
        HealthConnectAvailability.UNAVAILABLE -> HealthStatus(HealthConnectAccess.UNAVAILABLE, emptySet(), false)
        HealthConnectAvailability.UPDATE_REQUIRED -> HealthStatus(HealthConnectAccess.UPDATE_REQUIRED, emptySet(), false)
        HealthConnectAvailability.AVAILABLE -> {
            val granted = dataSource.grantedTypes()
            val access = if (granted.isEmpty()) HealthConnectAccess.NOT_CONNECTED else HealthConnectAccess.CONNECTED
            HealthStatus(access, granted, dataSource.hasHistoryPermission())
        }
    }

    override suspend fun sync() {
        val status = status()
        if (status.access != HealthConnectAccess.CONNECTED) return
        val today = clock.todayIn(zone)
        val isFirstSync = observeLastSync().first() == null
        val firstDay = today.minus(DatePeriod(days = daysToRead(isFirstSync, status.hasHistoryPermission) - 1))
        syncDays(status.grantedTypes, firstDay, today)
        if (WORKOUTS in status.grantedTypes) syncWorkouts(firstDay, today)
        preferences.edit { it[lastSyncKey] = clock.now().toEpochMilliseconds() }
    }

    override fun observeDays(from: LocalDate, toInclusive: LocalDate): Flow<List<HealthDay>> =
        dao.observeDays(from, toInclusive).map { days -> days.map { it.toModel() } }

    override fun observeWorkouts(): Flow<List<HealthWorkout>> =
        dao.observeWorkouts().map { workouts -> workouts.map { it.toModel() } }

    override fun observeLastSync(): Flow<Instant?> =
        preferences.data.map { data -> data[lastSyncKey]?.let(Instant::fromEpochMilliseconds) }

    private fun daysToRead(isFirstSync: Boolean, hasHistory: Boolean): Int = when {
        !isFirstSync -> RESYNC_DAYS
        hasHistory -> FIRST_SYNC_DAYS_WITH_HISTORY
        else -> FIRST_SYNC_DAYS
    }

    private suspend fun syncDays(granted: Set<HealthDataType>, from: LocalDate, today: LocalDate) {
        val readings = DayReadings.read(dataSource, granted, from, today, zone)
        val existing = dao.getDays(from, today).associateBy { it.date }
        val window = generateSequence(from) { it.plus(DatePeriod(days = 1)).takeIf { next -> next <= today } }.toList()
        val rows = window
            .map { date -> readings.applyTo(existing[date] ?: HealthDayEntity.empty(date), date, granted) }
            .filterNot { it.isEmpty() }
        dao.replaceDays(window, rows)
    }

    private suspend fun syncWorkouts(from: LocalDate, today: LocalDate) {
        val workouts = dataSource.readWorkouts(from, today)
            .filterNot { it.writtenByThisApp }
            .map { HealthWorkoutEntity(0, it.type, it.startedAt, it.durationMinutes, it.distanceM, it.kcal, it.averageHeartRate) }
        dao.replaceWorkouts(from.atStartOfDayIn(zone), today.plus(DatePeriod(days = 1)).atStartOfDayIn(zone), workouts)
    }

    private companion object {
        const val RESYNC_DAYS = 14
        const val FIRST_SYNC_DAYS = 30
        const val FIRST_SYNC_DAYS_WITH_HISTORY = 365
    }
}

private class DayReadings(
    val nutrition: Map<LocalDate, NutritionTotals>,
    val caloriesBurned: Map<LocalDate, Int>,
    val steps: Map<LocalDate, Int>,
    val weights: Map<LocalDate, Double>,
    val sleep: Map<LocalDate, Int>,
    val restingHeartRates: Map<LocalDate, Double>,
    val heartRateVariabilities: Map<LocalDate, Double>,
) {
    fun applyTo(base: HealthDayEntity, date: LocalDate, granted: Set<HealthDataType>): HealthDayEntity {
        var row = base
        if (NUTRITION in granted) {
            val totals = nutrition[date]
            row = row.copy(
                kcalEaten = totals?.kcal,
                proteinG = totals?.proteinG,
                carbsG = totals?.carbsG,
                sugarG = totals?.sugarG,
                fatG = totals?.fatG,
                saturatedFatG = totals?.saturatedFatG,
                fiberG = totals?.fiberG,
                sodiumMg = totals?.sodiumMg,
            )
        }
        if (CALORIES_BURNED in granted) row = row.copy(kcalBurned = caloriesBurned[date])
        if (STEPS in granted) row = row.copy(steps = steps[date])
        if (WEIGHT in granted) row = row.copy(weightKg = weights[date])
        if (SLEEP in granted) row = row.copy(sleepMinutes = sleep[date])
        if (RESTING_HEART_RATE in granted) row = row.copy(restingHeartRate = restingHeartRates[date])
        if (HEART_RATE_VARIABILITY in granted) row = row.copy(hrvMs = heartRateVariabilities[date])
        return row
    }

    companion object {
        suspend fun read(
            source: HealthDataSource,
            granted: Set<HealthDataType>,
            from: LocalDate,
            to: LocalDate,
            zone: TimeZone,
        ) = DayReadings(
            nutrition = if (NUTRITION in granted) source.readNutrition(from, to).associateBy { it.date } else emptyMap(),
            caloriesBurned = if (CALORIES_BURNED in granted) {
                source.readCaloriesBurned(from, to).associate { it.date to it.amount }
            } else {
                emptyMap()
            },
            steps = if (STEPS in granted) source.readSteps(from, to).associate { it.date to it.amount } else emptyMap(),
            weights = if (WEIGHT in granted) HealthDayRules.lastOfDay(source.readWeights(from, to), zone) else emptyMap(),
            sleep = if (SLEEP in granted) HealthDayRules.sleepOnWakingDay(source.readSleep(from, to), zone) else emptyMap(),
            restingHeartRates = if (RESTING_HEART_RATE in granted) {
                HealthDayRules.averageOfDay(source.readRestingHeartRates(from, to), zone)
            } else {
                emptyMap()
            },
            heartRateVariabilities = if (HEART_RATE_VARIABILITY in granted) {
                HealthDayRules.averageOfDay(source.readHeartRateVariabilities(from, to), zone)
            } else {
                emptyMap()
            },
        )
    }
}

private fun HealthDayEntity.toModel() = HealthDay(
    date, kcalEaten, proteinG, carbsG, sugarG, fatG, saturatedFatG, fiberG, sodiumMg,
    kcalBurned, weightKg, steps, sleepMinutes, restingHeartRate, hrvMs,
)

private fun HealthWorkoutEntity.toModel() =
    HealthWorkout(type, startedAt, durationMinutes, distanceM, kcal, averageHeartRate)
