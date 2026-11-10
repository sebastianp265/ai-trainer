package com.github.sebastianp265.aitrainer.integration.health

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

data class HealthStatus(
    val access: HealthConnectAccess,
    val grantedTypes: Set<HealthDataType>,
    val hasHistoryPermission: Boolean,
)

interface HealthService {
    val permissionsToRequest: Set<String>

    suspend fun status(): HealthStatus

    suspend fun sync()

    fun observeDays(from: LocalDate, toInclusive: LocalDate): Flow<List<HealthDay>>

    fun observeWorkouts(): Flow<List<HealthWorkout>>

    fun observeLastSync(): Flow<Instant?>
}
