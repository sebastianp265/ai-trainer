package com.github.sebastianp265.aitrainer.integration.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

interface SettingsService {
    fun observeShowTargets(): Flow<Boolean>

    suspend fun setShowTargets(show: Boolean)

    fun observeCoachEnabled(): Flow<Boolean>

    suspend fun setCoachEnabled(enabled: Boolean)

    fun observeLastReviewedWeek(): Flow<LocalDate?>

    suspend fun setLastReviewedWeek(weekStart: LocalDate)

    fun observeLastExport(): Flow<Instant?>

    suspend fun setLastExport(at: Instant)
}
