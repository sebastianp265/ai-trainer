package com.github.sebastianp265.aitrainer.integration.settings

import com.github.sebastianp265.aitrainer.integration.coach.ClaudeTuning
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

interface SettingsService {
    fun observeClaudeModel(): Flow<String>

    suspend fun setClaudeModel(modelId: String)

    fun observeClaudeTuning(): Flow<ClaudeTuning>

    suspend fun setClaudeTuning(tuning: ClaudeTuning)

    fun observeShowTargets(): Flow<Boolean>

    suspend fun setShowTargets(show: Boolean)

    fun observeCoachEnabled(): Flow<Boolean>

    suspend fun setCoachEnabled(enabled: Boolean)

    fun observeLastReviewedWeek(): Flow<LocalDate?>

    suspend fun setLastReviewedWeek(weekStart: LocalDate)

    fun observeLastExport(): Flow<Instant?>

    suspend fun setLastExport(at: Instant)

    fun observeApiKeyCheckedAt(): Flow<Instant?>

    suspend fun setApiKeyCheckedAt(checkedAt: Instant?)

    fun observeIsClaudeApiKeyStored(): Flow<Boolean>

    suspend fun saveClaudeApiKey(apiKey: String)

    suspend fun removeClaudeApiKey()

    companion object {
        const val DEFAULT_CLAUDE_MODEL = "claude-sonnet-5-5"
        val CLAUDE_MODELS = listOf("claude-opus-5-5", "claude-sonnet-5-5", "claude-haiku-5-5")
    }
}
