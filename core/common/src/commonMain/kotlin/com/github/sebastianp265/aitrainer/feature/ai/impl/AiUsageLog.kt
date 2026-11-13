package com.github.sebastianp265.aitrainer.feature.ai.impl

import com.github.sebastianp265.aitrainer.integration.ai.AiResponse
import com.github.sebastianp265.aitrainer.integration.ai.AiRun
import com.github.sebastianp265.aitrainer.integration.ai.AiUsage
import com.github.sebastianp265.aitrainer.shared.ai.AiDao
import com.github.sebastianp265.aitrainer.shared.ai.AiUsageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

internal class AiUsageLog(
    private val dao: AiDao,
    private val clock: Clock,
) {

    private val lastRun = MutableStateFlow<AiRun?>(null)

    fun observeUsageThisMonth(): Flow<AiUsage> =
        dao.observeUsage(currentMonth()).map { it?.let { u -> AiUsage(u.messages, u.inputTokens, u.outputTokens) } ?: AiUsage(0, 0, 0) }

    fun observeLastRun(): Flow<AiRun?> = lastRun

    suspend fun record(response: AiResponse, run: AiRun) {
        val month = currentMonth()
        val current = dao.usage(month) ?: AiUsageEntity(month, 0, 0, 0)
        val inputTokens = response.inputTokens + response.cacheReadTokens + response.cacheWriteTokens
        dao.upsertUsage(current.copy(messages = current.messages + 1, inputTokens = current.inputTokens + inputTokens, outputTokens = current.outputTokens + response.outputTokens))
        lastRun.value = run
    }

    private fun currentMonth(): String {
        val date = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return "${date.year}-${(date.month.ordinal + 1).toString().padStart(2, '0')}"
    }
}
