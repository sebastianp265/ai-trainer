package com.github.sebastianp265.aitrainer.integration.health

import kotlinx.datetime.LocalDate

enum class HealthConnectAvailability { AVAILABLE, UPDATE_REQUIRED, UNAVAILABLE }

interface HealthDataSource {
    val requiredPermissions: Set<String>
    fun availability(): HealthConnectAvailability
    suspend fun hasAllPermissions(): Boolean
    suspend fun readDailyTotals(from: LocalDate, toInclusive: LocalDate): List<HealthDay>
}
