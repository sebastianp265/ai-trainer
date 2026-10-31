package com.github.sebastianp265.aitrainer.testing

import com.github.sebastianp265.aitrainer.feature.health.data.HealthConnectAvailability
import com.github.sebastianp265.aitrainer.feature.health.data.HealthConnectDayDto
import com.github.sebastianp265.aitrainer.feature.health.data.HealthDataSource
import kotlinx.datetime.LocalDate

class FakeHealthDataSource(var days: List<HealthConnectDayDto>) : HealthDataSource {
    override val requiredPermissions: Set<String> = emptySet()
    override fun availability() = HealthConnectAvailability.AVAILABLE
    override suspend fun hasAllPermissions() = true
    override suspend fun readDailyTotals(from: LocalDate, toInclusive: LocalDate) = days.filter { it.date in from..toInclusive }
}
