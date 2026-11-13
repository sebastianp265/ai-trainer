package com.github.sebastianp265.aitrainer.feature.health.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
import com.github.sebastianp265.aitrainer.integration.health.HealthDay
import com.github.sebastianp265.aitrainer.integration.health.HealthService
import com.github.sebastianp265.aitrainer.integration.health.HealthStatus
import com.github.sebastianp265.aitrainer.integration.health.NutritionTotals
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

internal class HealthReloads {
    private val reloads = MutableStateFlow(0)
    val count: StateFlow<Int> = reloads.asStateFlow()

    fun next() = reloads.update { it + 1 }
}

internal class DefaultHealthService(
    private val dataSource: HealthDataSource,
    private val preferences: DataStore<Preferences>,
    private val reloads: HealthReloads,
    private val zone: TimeZone,
) : HealthService {

    private val disabledTypesKey = stringSetPreferencesKey("disabled_types")

    override suspend fun status(): HealthStatus = when (dataSource.availability()) {
        HealthConnectAvailability.UNAVAILABLE -> HealthStatus(HealthConnectAccess.UNAVAILABLE, emptySet(), false)
        HealthConnectAvailability.UPDATE_REQUIRED -> HealthStatus(HealthConnectAccess.UPDATE_REQUIRED, emptySet(), false)
        HealthConnectAvailability.AVAILABLE -> {
            val enabled = enabledTypes()
            val access = if (enabled.isEmpty()) HealthConnectAccess.NOT_CONNECTED else HealthConnectAccess.CONNECTED
            HealthStatus(access, enabled, dataSource.missingHistoryPermissions().isEmpty())
        }
    }

    override suspend fun enable(type: HealthDataType): Set<String> {
        setDisabled(type, false)
        return dataSource.missingPermissions(type)
    }

    override suspend fun disable(type: HealthDataType) = setDisabled(type, true)

    override suspend fun missingHistoryPermissions(): Set<String> = dataSource.missingHistoryPermissions()

    override fun reload() = reloads.next()

    override fun observeDays(from: LocalDate, toInclusive: LocalDate): Flow<List<HealthDay>> =
        reloads.count.map { readOrNothing { readDays(from, toInclusive) } }

    private suspend fun setDisabled(type: HealthDataType, disabled: Boolean) {
        preferences.edit { data ->
            val types = data.disabledTypes().let { if (disabled) it + type else it - type }
            data[disabledTypesKey] = types.map { it.name }.toSet()
        }
        reload()
    }

    private suspend fun enabledTypes(): Set<HealthDataType> =
        if (dataSource.availability() == HealthConnectAvailability.AVAILABLE) {
            dataSource.grantedTypes() - preferences.data.first().disabledTypes()
        } else {
            emptySet()
        }

    private suspend fun readDays(from: LocalDate, toInclusive: LocalDate): List<HealthDay> {
        val enabled = enabledTypes()
        if (enabled.isEmpty()) return emptyList()
        val readings = DayReadings.read(dataSource, enabled, from, toInclusive, zone)
        return readings.dates()
            .filter { it in from..toInclusive }
            .sortedDescending()
            .map(readings::dayOf)
            .filter { it.hasAnyValue() }
    }

    private suspend fun <T> readOrNothing(read: suspend () -> List<T>): List<T> = try {
        read()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        emptyList()
    }

    private fun Preferences.disabledTypes(): Set<HealthDataType> {
        val names = this[disabledTypesKey] ?: emptySet()
        return HealthDataType.entries.filter { it.name in names }.toSet()
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
    fun dates(): Set<LocalDate> =
        nutrition.keys + caloriesBurned.keys + steps.keys + weights.keys + sleep.keys + restingHeartRates.keys + heartRateVariabilities.keys

    fun dayOf(date: LocalDate): HealthDay {
        val totals = nutrition[date]
        return HealthDay(
            date = date,
            kcalEaten = totals?.kcal,
            proteinG = totals?.proteinG,
            carbsG = totals?.carbsG,
            sugarG = totals?.sugarG,
            fatG = totals?.fatG,
            saturatedFatG = totals?.saturatedFatG,
            fiberG = totals?.fiberG,
            sodiumMg = totals?.sodiumMg,
            kcalBurned = caloriesBurned[date],
            weightKg = weights[date],
            steps = steps[date],
            sleepMinutes = sleep[date],
            restingHeartRate = restingHeartRates[date],
            hrvMs = heartRateVariabilities[date],
        )
    }

    companion object {
        suspend fun read(
            source: HealthDataSource,
            enabled: Set<HealthDataType>,
            from: LocalDate,
            to: LocalDate,
            zone: TimeZone,
        ) = DayReadings(
            nutrition = if (NUTRITION in enabled) source.readNutrition(from, to).associateBy { it.date } else emptyMap(),
            caloriesBurned = if (CALORIES_BURNED in enabled) {
                source.readCaloriesBurned(from, to).associate { it.date to it.amount }
            } else {
                emptyMap()
            },
            steps = if (STEPS in enabled) source.readSteps(from, to).associate { it.date to it.amount } else emptyMap(),
            weights = if (WEIGHT in enabled) HealthDayRules.lastOfDay(source.readWeights(from, to), zone) else emptyMap(),
            sleep = if (SLEEP in enabled) HealthDayRules.sleepOnWakingDay(source.readSleep(from, to), zone) else emptyMap(),
            restingHeartRates = if (RESTING_HEART_RATE in enabled) {
                HealthDayRules.averageOfDay(source.readRestingHeartRates(from, to), zone)
            } else {
                emptyMap()
            },
            heartRateVariabilities = if (HEART_RATE_VARIABILITY in enabled) {
                HealthDayRules.averageOfDay(source.readHeartRateVariabilities(from, to), zone)
            } else {
                emptyMap()
            },
        )
    }
}

private fun HealthDay.hasAnyValue(): Boolean =
    listOf(kcalEaten, kcalBurned, steps, sleepMinutes).any { it != null } ||
        listOf(proteinG, carbsG, sugarG, fatG, saturatedFatG, fiberG, sodiumMg, weightKg, restingHeartRate, hrvMs).any { it != null }
