package com.github.sebastianp265.aitrainer.integration.settings

import kotlinx.coroutines.flow.Flow

interface SettingsService {
    fun observeClaudeModel(): Flow<String>

    suspend fun setClaudeModel(modelId: String)

    fun observeShowTargets(): Flow<Boolean>

    suspend fun setShowTargets(show: Boolean)

    fun observeCoachEnabled(): Flow<Boolean>

    suspend fun setCoachEnabled(enabled: Boolean)

    fun observeIsClaudeApiKeyStored(): Flow<Boolean>

    suspend fun saveClaudeApiKey(apiKey: String)

    suspend fun removeClaudeApiKey()

    companion object {
        const val DEFAULT_CLAUDE_MODEL = "claude-sonnet-5-5"
    }
}
