package com.github.sebastianp265.aitrainer.integration.health

interface HealthService {
    val requiredPermissions: Set<String>

    suspend fun checkAccess(): HealthConnectAccess

    suspend fun readRecentDays(): List<HealthDay>?
}
