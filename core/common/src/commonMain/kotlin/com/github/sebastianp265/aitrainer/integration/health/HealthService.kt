package com.github.sebastianp265.aitrainer.integration.health

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

data class HealthStatus(
    val access: HealthConnectAccess,
    val enabledTypes: Set<HealthDataType>,
    val hasHistoryAccess: Boolean,
)

interface HealthService {
    suspend fun status(): HealthStatus

    suspend fun enable(type: HealthDataType): Set<String>

    suspend fun disable(type: HealthDataType)

    suspend fun missingHistoryPermissions(): Set<String>

    fun reload()

    fun observeDays(from: LocalDate, toInclusive: LocalDate): Flow<List<HealthDay>>
}
