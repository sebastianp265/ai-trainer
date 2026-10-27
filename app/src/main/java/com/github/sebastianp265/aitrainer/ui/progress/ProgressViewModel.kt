package com.github.sebastianp265.aitrainer.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.data.healthconnect.HealthConnectAvailability
import com.github.sebastianp265.aitrainer.data.healthconnect.HealthDataSource
import com.github.sebastianp265.aitrainer.data.repo.BodyWeightRepository
import com.github.sebastianp265.aitrainer.data.repo.EnergyRepository
import com.github.sebastianp265.aitrainer.data.repo.WorkoutRepository
import com.github.sebastianp265.aitrainer.domain.model.DailyEnergy
import com.github.sebastianp265.aitrainer.domain.model.WeightMeasurement
import com.github.sebastianp265.aitrainer.domain.stats.DailyWeight
import com.github.sebastianp265.aitrainer.domain.stats.EnergyAverages
import com.github.sebastianp265.aitrainer.domain.stats.MuscleGroupVolume
import com.github.sebastianp265.aitrainer.domain.stats.WeeklyBalance
import com.github.sebastianp265.aitrainer.domain.stats.balanceKcal
import com.github.sebastianp265.aitrainer.domain.stats.dailyAverageWeights
import com.github.sebastianp265.aitrainer.domain.stats.energyAverages
import com.github.sebastianp265.aitrainer.domain.stats.setsPerMuscleGroup
import com.github.sebastianp265.aitrainer.domain.stats.sevenDayMovingAverage
import com.github.sebastianp265.aitrainer.domain.stats.weeklyBalances
import com.github.sebastianp265.aitrainer.domain.stats.weeklyWeightChangeKg
import com.github.sebastianp265.aitrainer.sync.HealthConnectSyncScheduler
import com.github.sebastianp265.aitrainer.ui.WhileUiSubscribed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

enum class HealthConnectStatus { CHECKING, UNAVAILABLE, UPDATE_REQUIRED, NOT_CONNECTED, CONNECTED }

data class WeightSummary(
    val latestKg: Double?,
    val sevenDayAverageKg: Double?,
    val weeklyChangeKg: Double?,
)

data class DailyBalance(val date: LocalDate, val balanceKcal: Int)

data class ProgressUiState(
    val isLoading: Boolean = true,
    val chartStart: LocalDate = LocalDate(1970, 1, 1),
    val healthConnectStatus: HealthConnectStatus = HealthConnectStatus.CHECKING,
    val healthConnectPermissions: Set<String> = emptySet(),
    val weightSummary: WeightSummary = WeightSummary(null, null, null),
    val dailyWeights: List<DailyWeight> = emptyList(),
    val weightMovingAverage: List<DailyWeight> = emptyList(),
    val recentMeasurements: List<WeightMeasurement> = emptyList(),
    val lastSevenDaysEnergy: EnergyAverages = EnergyAverages(null, null, null),
    val dailyBalances: List<DailyBalance> = emptyList(),
    val weeklyBalances: List<WeeklyBalance> = emptyList(),
    val lastSevenDaysVolume: List<MuscleGroupVolume> = emptyList(),
)

class ProgressViewModel(
    private val bodyWeightRepository: BodyWeightRepository,
    energyRepository: EnergyRepository,
    workoutRepository: WorkoutRepository,
    private val healthDataSource: HealthDataSource,
    private val healthConnectSyncScheduler: HealthConnectSyncScheduler,
    clock: Clock,
) : ViewModel() {

    private companion object {
        const val CHART_DAYS = 42
        const val DAILY_BALANCE_DAYS = 28
        const val SUMMARY_DAYS = 7
        const val RECENT_MEASUREMENTS = 5
    }

    private val timeZone = TimeZone.currentSystemDefault()
    private val today = clock.todayIn(timeZone)
    private val chartStart = today.daysAgo(CHART_DAYS - 1)
    private val healthConnectStatus = MutableStateFlow(HealthConnectStatus.CHECKING)

    val uiState: StateFlow<ProgressUiState> = combine(
        bodyWeightRepository.observeSince(chartStart.atStartOfDayIn(timeZone)),
        energyRepository.observeRange(chartStart, today),
        workoutRepository.observeFinishedSetsSince(today.daysAgo(SUMMARY_DAYS - 1)),
        healthConnectStatus,
    ) { measurements, energy, sets, status ->
        val dailyWeights = dailyAverageWeights(measurements, timeZone)
        val movingAverage = sevenDayMovingAverage(dailyWeights)
        ProgressUiState(
            isLoading = false,
            chartStart = chartStart,
            healthConnectStatus = status,
            healthConnectPermissions = healthDataSource.requiredPermissions,
            weightSummary = WeightSummary(
                latestKg = measurements.maxByOrNull { it.measuredAt }?.weightKg,
                sevenDayAverageKg = movingAverage.lastOrNull()?.weightKg,
                weeklyChangeKg = weeklyWeightChangeKg(dailyWeights),
            ),
            dailyWeights = dailyWeights,
            weightMovingAverage = movingAverage,
            recentMeasurements = measurements.sortedByDescending { it.measuredAt }.take(RECENT_MEASUREMENTS),
            lastSevenDaysEnergy = energyAverages(energy.completeDaysBeforeToday(SUMMARY_DAYS)),
            dailyBalances = energy.filter { it.date >= today.daysAgo(DAILY_BALANCE_DAYS - 1) }
                .mapNotNull { day -> day.balanceKcal()?.let { DailyBalance(day.date, it) } },
            weeklyBalances = weeklyBalances(energy),
            lastSevenDaysVolume = setsPerMuscleGroup(sets),
        )
    }.stateIn(viewModelScope, WhileUiSubscribed, ProgressUiState())

    fun refreshHealthConnectStatus() {
        viewModelScope.launch { healthConnectStatus.value = currentHealthConnectStatus() }
    }

    fun onHealthConnectPermissionsResult() {
        viewModelScope.launch {
            healthConnectStatus.value = currentHealthConnectStatus()
            if (healthConnectStatus.value == HealthConnectStatus.CONNECTED) healthConnectSyncScheduler.syncNow()
        }
    }

    fun syncHealthConnectNow() = healthConnectSyncScheduler.syncNow()

    fun addWeight(weightKg: Double) {
        viewModelScope.launch { bodyWeightRepository.addManualMeasurement(weightKg) }
    }

    fun deleteMeasurement(measurement: WeightMeasurement) {
        viewModelScope.launch { bodyWeightRepository.delete(measurement.id) }
    }

    private suspend fun currentHealthConnectStatus(): HealthConnectStatus = when (healthDataSource.availability()) {
        HealthConnectAvailability.UNAVAILABLE -> HealthConnectStatus.UNAVAILABLE
        HealthConnectAvailability.UPDATE_REQUIRED -> HealthConnectStatus.UPDATE_REQUIRED
        HealthConnectAvailability.AVAILABLE ->
            if (healthDataSource.hasAllPermissions()) HealthConnectStatus.CONNECTED else HealthConnectStatus.NOT_CONNECTED
    }

    private fun List<DailyEnergy>.completeDaysBeforeToday(days: Int): List<DailyEnergy> {
        val firstDay = today.daysAgo(days)
        return filter { it.date >= firstDay && it.date < today }
    }

    private fun LocalDate.daysAgo(days: Int): LocalDate = minus(DatePeriod(days = days))
}
