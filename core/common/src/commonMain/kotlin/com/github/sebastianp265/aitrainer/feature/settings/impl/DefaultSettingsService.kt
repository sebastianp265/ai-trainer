package com.github.sebastianp265.aitrainer.feature.settings.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.sebastianp265.aitrainer.integration.settings.SettingsService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

internal class DefaultSettingsService(
    private val settings: DataStore<Preferences>,
) : SettingsService {

    private val showTargetsKey = booleanPreferencesKey("show_targets")
    private val coachEnabledKey = booleanPreferencesKey("coach_enabled")
    private val lastReviewedWeekKey = stringPreferencesKey("last_reviewed_week")
    private val lastExportKey = longPreferencesKey("last_export")

    override fun observeShowTargets(): Flow<Boolean> = settings.data.map { it[showTargetsKey] ?: true }

    override suspend fun setShowTargets(show: Boolean) {
        settings.edit { it[showTargetsKey] = show }
    }

    override fun observeCoachEnabled(): Flow<Boolean> = settings.data.map { it[coachEnabledKey] ?: true }

    override fun observeLastReviewedWeek(): Flow<LocalDate?> = settings.data.map { preferences -> preferences[lastReviewedWeekKey]?.let(LocalDate::parse) }

    override suspend fun setLastReviewedWeek(weekStart: LocalDate) {
        settings.edit { it[lastReviewedWeekKey] = weekStart.toString() }
    }

    override fun observeLastExport(): Flow<Instant?> =
        settings.data.map { preferences -> preferences[lastExportKey]?.let(Instant::fromEpochMilliseconds) }

    override suspend fun setLastExport(at: Instant) {
        settings.edit { it[lastExportKey] = at.toEpochMilliseconds() }
    }

    override suspend fun setCoachEnabled(enabled: Boolean) {
        settings.edit { it[coachEnabledKey] = enabled }
    }
}
