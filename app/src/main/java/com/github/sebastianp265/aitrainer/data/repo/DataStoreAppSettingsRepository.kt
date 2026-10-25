package com.github.sebastianp265.aitrainer.data.repo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreAppSettingsRepository(
    private val settings: DataStore<Preferences>,
) : AppSettingsRepository {

    private val claudeModelKey = stringPreferencesKey("claude_model")

    override fun observeClaudeModel(): Flow<String> =
        settings.data.map { it[claudeModelKey]?.ifBlank { null } ?: AppSettingsRepository.DEFAULT_CLAUDE_MODEL }

    override suspend fun setClaudeModel(modelId: String) {
        settings.edit { it[claudeModelKey] = modelId.trim() }
    }
}
