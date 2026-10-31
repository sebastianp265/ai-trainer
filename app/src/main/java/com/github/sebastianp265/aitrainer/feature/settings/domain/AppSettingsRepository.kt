package com.github.sebastianp265.aitrainer.feature.settings.domain

import kotlinx.coroutines.flow.Flow

interface AppSettingsRepository {
    fun observeClaudeModel(): Flow<String>
    suspend fun setClaudeModel(modelId: String)

    companion object {
        const val DEFAULT_CLAUDE_MODEL = "claude-sonnet-5-5"
    }
}
