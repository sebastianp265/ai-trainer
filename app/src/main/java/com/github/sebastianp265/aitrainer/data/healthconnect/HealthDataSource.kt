package com.github.sebastianp265.aitrainer.data.healthconnect

import kotlinx.datetime.LocalDate

enum class HealthConnectAvailability { AVAILABLE, UPDATE_REQUIRED, UNAVAILABLE }

data class HealthConnectDayDto(
    val date: LocalDate,
    val kcalEaten: Int?,
    val proteinG: Double?,
    val carbsG: Double?,
    val fatG: Double?,
    val sourcePackages: List<String>,
)

interface HealthDataSource {
    val requiredPermissions: Set<String>
    fun availability(): HealthConnectAvailability
    suspend fun hasAllPermissions(): Boolean
    suspend fun readDailyTotals(from: LocalDate, toInclusive: LocalDate): List<HealthConnectDayDto>
}
