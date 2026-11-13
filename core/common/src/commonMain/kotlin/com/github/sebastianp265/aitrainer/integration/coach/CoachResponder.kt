package com.github.sebastianp265.aitrainer.integration.coach

import kotlinx.coroutines.flow.Flow

interface CoachResponder {
    fun observeReadiness(): Flow<CoachReadiness>

    suspend fun reply(request: CoachRequest, desk: ProposalDesk, onProgress: (CoachStep?) -> Unit): CoachReply
}

data class CoachTurn(val role: CoachRole, val text: String, val topic: CoachTopic?)

data class CoachRequest(val turns: List<CoachTurn>, val review: WeeklyReviewSummary? = null)

sealed interface CoachReply {
    data class Answer(val text: String, val checked: List<String>) : CoachReply

    data class Failure(val kind: CoachFailure, val message: String) : CoachReply
}
