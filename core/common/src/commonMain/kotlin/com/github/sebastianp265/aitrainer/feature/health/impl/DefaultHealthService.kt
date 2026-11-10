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

    override val requiredPermissions: Set<String> get() = healthDataSource.permissionsToRequest

    override suspend fun checkAccess(): HealthConnectAccess = when (healthDataSource.availability()) {
        HealthConnectAvailability.UNAVAILABLE -> HealthConnectAccess.UNAVAILABLE
        HealthConnectAvailability.UPDATE_REQUIRED -> HealthConnectAccess.UPDATE_REQUIRED
        HealthConnectAvailability.AVAILABLE ->
            if (healthDataSource.grantedTypes().isNotEmpty()) HealthConnectAccess.CONNECTED else HealthConnectAccess.NOT_CONNECTED
    }

    override suspend fun readRecentDays(): List<HealthDay>? {
        if (checkAccess() != HealthConnectAccess.CONNECTED) return null
        val today = clock.todayIn(TimeZone.currentSystemDefault())
        val firstDay = today.minus(DatePeriod(days = DAYS - 1))
        return healthDataSource.readNutrition(firstDay, today)
            .map { HealthDay(it.date, it.kcal, it.proteinG, it.carbsG, it.fatG, emptyList()) }
            .filter { day -> listOf(day.kcalEaten, day.proteinG, day.carbsG, day.fatG).any { it != null } }
            .sortedByDescending { it.date }
    }
}
