package com.github.sebastianp265.aitrainer.integration.coach

import kotlinx.coroutines.flow.Flow

interface CoachService {
    fun observeMessages(): Flow<List<CoachMessage>>

    fun observeActivity(): Flow<CoachActivity>

    suspend fun send(text: String)

    suspend fun retry()

    fun observePendingProposals(): Flow<List<CoachProposal>>

    fun observeProposal(id: Long): Flow<CoachProposal?>

    suspend fun accept(proposalId: Long): Boolean

    suspend fun acceptAll(proposalIds: List<Long>)

    fun observeProfile(): Flow<Map<String, String>>

    suspend fun setProfileField(field: String, value: String)

    fun observeNotes(): Flow<List<CoachNote>>

    suspend fun updateNote(id: Long, text: String)

    suspend fun deleteNote(id: Long)

    fun observeUsageThisMonth(): Flow<CoachUsage>

    fun observeLog(): Flow<List<CoachLogEntry>>

    suspend fun clearLog()

    companion object {
        val PROFILE_FIELDS = listOf("goal", "time", "equipment", "limitations", "preferences")
    }
}

sealed interface CoachActivity {
    data object Idle : CoachActivity

    data class Working(val tool: String?) : CoachActivity

    data class Failed(val kind: ClaudeFailureKind, val message: String) : CoachActivity
}
