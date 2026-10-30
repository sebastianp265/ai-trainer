package com.github.sebastianp265.aitrainer.data.repo

import kotlinx.coroutines.flow.Flow

interface AppSettingsRepository {
    fun observeClaudeModel(): Flow<String>
    suspend fun setClaudeModel(modelId: String)

    companion object {
        const val DEFAULT_CLAUDE_MODEL = "claude-sonnet-5-5"
    }
}
