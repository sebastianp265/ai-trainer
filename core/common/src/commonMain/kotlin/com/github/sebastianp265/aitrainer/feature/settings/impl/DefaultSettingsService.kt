package com.github.sebastianp265.aitrainer.feature.settings.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.sebastianp265.aitrainer.integration.settings.Secret
import com.github.sebastianp265.aitrainer.integration.settings.SecretStore
import com.github.sebastianp265.aitrainer.integration.settings.SettingsService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class DefaultSettingsService(
    private val settings: DataStore<Preferences>,
    private val secretStore: SecretStore,
) : SettingsService {

    private val claudeModelKey = stringPreferencesKey("claude_model")
    private val showTargetsKey = booleanPreferencesKey("show_targets")
    private val coachEnabledKey = booleanPreferencesKey("coach_enabled")

    override fun observeShowTargets(): Flow<Boolean> = settings.data.map { it[showTargetsKey] ?: true }

    override suspend fun setShowTargets(show: Boolean) {
        settings.edit { it[showTargetsKey] = show }
    }

    override fun observeCoachEnabled(): Flow<Boolean> = settings.data.map { it[coachEnabledKey] ?: false }

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
