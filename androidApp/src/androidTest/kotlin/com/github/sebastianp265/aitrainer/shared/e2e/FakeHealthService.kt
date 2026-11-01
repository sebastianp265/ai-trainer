package com.github.sebastianp265.aitrainer.shared.e2e

import com.github.sebastianp265.aitrainer.integration.health.HealthConnectAccess
import com.github.sebastianp265.aitrainer.integration.health.HealthDay
import com.github.sebastianp265.aitrainer.integration.health.HealthService

class FakeHealthService(var days: List<HealthDay>) : HealthService {
    override val requiredPermissions: Set<String> = emptySet()
    override suspend fun checkAccess() = HealthConnectAccess.CONNECTED
    override suspend fun readRecentDays() = days.sortedByDescending { it.date }
}
