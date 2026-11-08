package com.github.sebastianp265.aitrainer.feature.health.impl

import com.github.sebastianp265.aitrainer.integration.health.HealthConnectAccess
import com.github.sebastianp265.aitrainer.integration.health.HealthConnectAvailability
import com.github.sebastianp265.aitrainer.integration.health.HealthDataSource
import com.github.sebastianp265.aitrainer.integration.health.HealthDay
import com.github.sebastianp265.aitrainer.integration.health.HealthService
import kotlin.time.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn

internal class DefaultHealthService(
    private val healthDataSource: HealthDataSource,
    private val clock: Clock,
) : HealthService {
    private companion object {
        const val DAYS = 30
    }

    override val requiredPermissions: Set<String> get() = healthDataSource.requiredPermissions

    override suspend fun checkAccess(): HealthConnectAccess = when (healthDataSource.availability()) {
        HealthConnectAvailability.UNAVAILABLE -> HealthConnectAccess.UNAVAILABLE
        HealthConnectAvailability.UPDATE_REQUIRED -> HealthConnectAccess.UPDATE_REQUIRED
        HealthConnectAvailability.AVAILABLE ->
            if (healthDataSource.hasAllPermissions()) HealthConnectAccess.CONNECTED else HealthConnectAccess.NOT_CONNECTED
    }

    override suspend fun readRecentDays(): List<HealthDay>? {
        if (checkAccess() != HealthConnectAccess.CONNECTED) return null
        val today = clock.todayIn(TimeZone.currentSystemDefault())
        val firstDay = today.minus(DatePeriod(days = DAYS - 1))
        return healthDataSource.readDailyTotals(firstDay, today)
            .filter { it.hasAnyValue() }
            .sortedByDescending { it.date }
    }

    private fun HealthDay.hasAnyValue() =
        listOf(kcalEaten, proteinG, carbsG, fatG).any { it != null }
}
