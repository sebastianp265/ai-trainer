package com.github.sebastianp265.aitrainer.testing

import com.github.sebastianp265.aitrainer.data.healthconnect.HealthConnectAvailability
import com.github.sebastianp265.aitrainer.data.healthconnect.HealthDataSource
import com.github.sebastianp265.aitrainer.domain.model.HealthDayTotals
import kotlinx.datetime.LocalDate

class FakeHealthDataSource(var days: List<HealthDayTotals>) : HealthDataSource {
    override val requiredPermissions: Set<String> = emptySet()
    override fun availability() = HealthConnectAvailability.AVAILABLE
    override suspend fun hasAllPermissions() = true
    override suspend fun readDailyTotals(from: LocalDate, toInclusive: LocalDate) = days.filter { it.date in from..toInclusive }
}
