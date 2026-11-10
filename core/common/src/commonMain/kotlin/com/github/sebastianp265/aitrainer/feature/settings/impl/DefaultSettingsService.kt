package com.github.sebastianp265.aitrainer.feature.settings.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.sebastianp265.aitrainer.integration.settings.Secret
import com.github.sebastianp265.aitrainer.integration.settings.SecretStore
import com.github.sebastianp265.aitrainer.integration.settings.SettingsService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

internal class DefaultSettingsService(
    private val settings: DataStore<Preferences>,
    private val secretStore: SecretStore,
) : SettingsService {

    private val claudeModelKey = stringPreferencesKey("claude_model")
    private val showTargetsKey = booleanPreferencesKey("show_targets")
    private val coachEnabledKey = booleanPreferencesKey("coach_enabled")
    private val fakeClaudeKey = booleanPreferencesKey("fake_claude")
    private val fakeHealthKey = booleanPreferencesKey("fake_health")
    private val lastReviewedWeekKey = stringPreferencesKey("last_reviewed_week")
    private val lastExportKey = longPreferencesKey("last_export")
    private val apiKeyCheckedAtKey = longPreferencesKey("api_key_checked_at")

    override fun observeShowTargets(): Flow<Boolean> = settings.data.map { it[showTargetsKey] ?: true }

    override suspend fun setShowTargets(show: Boolean) {
        settings.edit { it[showTargetsKey] = show }
    }

    override fun observeCoachEnabled(): Flow<Boolean> = settings.data.map { it[coachEnabledKey] ?: true }

    override fun observeUseFakeClaude(): Flow<Boolean> = settings.data.map { it[fakeClaudeKey] ?: false }

    override suspend fun setUseFakeClaude(use: Boolean) {
        settings.edit { it[fakeClaudeKey] = use }
    }

    override fun observeUseFakeHealth(): Flow<Boolean> = settings.data.map { it[fakeHealthKey] ?: false }

    override suspend fun setUseFakeHealth(use: Boolean) {
        settings.edit { it[fakeHealthKey] = use }
    }

    override fun observeLastReviewedWeek(): Flow<LocalDate?> = settings.data.map { preferences -> preferences[lastReviewedWeekKey]?.let(LocalDate::parse) }

    override suspend fun setLastReviewedWeek(weekStart: LocalDate) {
        settings.edit { it[lastReviewedWeekKey] = weekStart.toString() }
    }

    override fun observeLastExport(): Flow<Instant?> =
        settings.data.map { preferences -> preferences[lastExportKey]?.let(Instant::fromEpochMilliseconds) }

    override suspend fun setLastExport(at: Instant) {
        settings.edit { it[lastExportKey] = at.toEpochMilliseconds() }
    }

    override fun observeApiKeyCheckedAt(): Flow<Instant?> =
        settings.data.map { preferences -> preferences[apiKeyCheckedAtKey]?.let(Instant::fromEpochMilliseconds) }

    override suspend fun setApiKeyCheckedAt(checkedAt: Instant?) {
        settings.edit { preferences ->
            if (checkedAt == null) preferences.remove(apiKeyCheckedAtKey) else preferences[apiKeyCheckedAtKey] = checkedAt.toEpochMilliseconds()
        }
    }

    override suspend fun setCoachEnabled(enabled: Boolean) {
        settings.edit { it[coachEnabledKey] = enabled }
    }

    override fun observeClaudeModel(): Flow<String> =
        settings.data.map { it[claudeModelKey]?.ifBlank { null } ?: SettingsService.DEFAULT_CLAUDE_MODEL }

    override suspend fun setClaudeModel(modelId: String) {
        settings.edit { it[claudeModelKey] = modelId.trim() }
    }

    override fun observeIsClaudeApiKeyStored(): Flow<Boolean> = secretStore.observeIsStored(Secret.CLAUDE_API_KEY)

    override suspend fun saveClaudeApiKey(apiKey: String) = secretStore.write(Secret.CLAUDE_API_KEY, apiKey)

    override suspend fun removeClaudeApiKey() = secretStore.delete(Secret.CLAUDE_API_KEY)
}
