package com.github.sebastianp265.aitrainer.integration.coach

import com.github.sebastianp265.aitrainer.integration.plan.PlannedSet
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

enum class CoachRole { USER, COACH }

enum class ProposalType { NEW_PLAN, CHANGE_PLAN, MULTI_PLAN, TARGET }

enum class ProposalState { PENDING, ACCEPTED, SUPERSEDED }

enum class CoachLogKind { REQUEST, TOOL, ERROR }

data class ProposedPlan(
    val planId: Long?,
    val name: String,
    val date: LocalDate?,
    val sets: List<PlannedSet>,
)

data class ProposedTarget(
    val groupKey: String,
    val minSets: Int,
    val maxSets: Int,
    val reason: String?,
    val previousMinSets: Int,
    val previousMaxSets: Int,
)

data class CoachProposal(
    val id: Long,
    val type: ProposalType,
    val state: ProposalState,
    val summary: String,
    val plans: List<ProposedPlan>,
    val target: ProposedTarget?,
    val createdAt: Instant,
)

data class CoachMessage(
    val id: Long,
    val role: CoachRole,
    val text: String,
    val createdAt: Instant,
    val checked: List<String>,
    val proposals: List<CoachProposal>,
)

data class CoachNote(val id: Long, val text: String, val createdAt: Instant)

data class CoachUsage(val messages: Int, val inputTokens: Long, val outputTokens: Long)

data class CoachLogEntry(val id: Long, val at: Instant, val kind: CoachLogKind, val summary: String, val details: String)
