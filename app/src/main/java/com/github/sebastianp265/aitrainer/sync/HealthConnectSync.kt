package com.github.sebastianp265.aitrainer.sync

import com.github.sebastianp265.aitrainer.data.healthconnect.HealthConnectAvailability
import com.github.sebastianp265.aitrainer.data.healthconnect.HealthDataSource
import com.github.sebastianp265.aitrainer.data.repo.EnergyRepository
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class HealthConnectSync(
    private val healthDataSource: HealthDataSource,
    private val energyRepository: EnergyRepository,
    private val clock: Clock,
) {
    private companion object {
        const val DAYS_TO_SYNC = 30
    }

    suspend fun syncRecentDays() {
        if (healthDataSource.availability() != HealthConnectAvailability.AVAILABLE) return
        if (!healthDataSource.hasAllPermissions()) return
        val today = clock.todayIn(TimeZone.currentSystemDefault())
        val firstDay = today.minus(DatePeriod(days = DAYS_TO_SYNC - 1))
        energyRepository.replaceHealthConnectDays(firstDay, today, healthDataSource.readDailyTotals(firstDay, today))
    }
}
