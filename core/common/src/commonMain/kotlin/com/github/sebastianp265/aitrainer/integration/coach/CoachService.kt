package com.github.sebastianp265.aitrainer.integration.coach

import kotlinx.coroutines.flow.Flow

interface CoachService {
    fun observeMessages(): Flow<List<CoachMessage>>

    fun observeActivity(): Flow<CoachActivity>

    fun observeReadiness(): Flow<CoachReadiness>

    suspend fun send(text: String, topic: CoachTopic? = null)

    suspend fun retry()

    suspend fun deleteMessage(id: Long)

    suspend fun reviewLastWeekIfDue()

    fun observePendingProposals(): Flow<List<CoachProposal>>

    fun observeProposal(id: Long): Flow<CoachProposal?>

    suspend fun accept(proposalId: Long): Boolean

    suspend fun acceptAll(proposalIds: List<Long>)

    suspend fun undo(proposalId: Long): Boolean
}

sealed interface CoachActivity {
    data object Idle : CoachActivity

    data class Working(val step: CoachStep?) : CoachActivity

    data class Failed(val kind: CoachFailure, val message: String) : CoachActivity
}

enum class CoachReadiness { READY, NEEDS_SETUP }

enum class CoachFailure { NEEDS_SETUP, NO_CONNECTION, BUSY, OTHER }

enum class CoachStep { WORKOUTS, EXERCISE_HISTORY, HEALTH, PLANS, WEEKLY_SETS, WORKOUT, EXERCISES, WRITING }
