package com.github.sebastianp265.aitrainer.feature.settings.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeCacheDuration
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeEffort
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeThinking
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeTuning
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
    private val lastReviewedWeekKey = stringPreferencesKey("last_reviewed_week")
    private val lastExportKey = longPreferencesKey("last_export")
    private val apiKeyCheckedAtKey = longPreferencesKey("api_key_checked_at")
    private val effortKey = stringPreferencesKey("claude_effort")
    private val thinkingKey = stringPreferencesKey("claude_thinking")
    private val maxTokensKey = intPreferencesKey("claude_max_tokens")
    private val cacheDurationKey = stringPreferencesKey("claude_cache_duration")

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

    override fun observeClaudeTuning(): Flow<ClaudeTuning> = settings.data.map { preferences ->
        val defaults = ClaudeTuning()
        ClaudeTuning(
            effort = preferences[effortKey]?.let(ClaudeEffort::valueOf),
            thinking = preferences[thinkingKey]?.let(ClaudeThinking::valueOf) ?: defaults.thinking,
            maxTokens = preferences[maxTokensKey] ?: defaults.maxTokens,
            cacheDuration = preferences[cacheDurationKey]?.let(ClaudeCacheDuration::valueOf) ?: defaults.cacheDuration,
        )
    }

    override suspend fun setClaudeTuning(tuning: ClaudeTuning) {
        settings.edit { preferences ->
            val effort = tuning.effort
            if (effort == null) preferences.remove(effortKey) else preferences[effortKey] = effort.name
            preferences[thinkingKey] = tuning.thinking.name
            preferences[maxTokensKey] = tuning.maxTokens
            preferences[cacheDurationKey] = tuning.cacheDuration.name
        }
    }

    override fun observeIsClaudeApiKeyStored(): Flow<Boolean> = secretStore.observeIsStored(Secret.CLAUDE_API_KEY)

    override suspend fun saveClaudeApiKey(apiKey: String) = secretStore.write(Secret.CLAUDE_API_KEY, apiKey)

    override suspend fun removeClaudeApiKey() = secretStore.delete(Secret.CLAUDE_API_KEY)
}
