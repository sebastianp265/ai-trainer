package com.github.sebastianp265.aitrainer.integration.coach

import com.github.sebastianp265.aitrainer.integration.exercise.NewExercise
import com.github.sebastianp265.aitrainer.integration.plan.PlannedSet
import kotlinx.datetime.LocalDate
import kotlin.time.Duration
import kotlin.time.Instant

enum class CoachRole { USER, COACH }

enum class TopicKind { PLAN, WORKOUT, ACTIVE_WORKOUT, EXERCISE }

data class CoachTopic(val kind: TopicKind, val id: String, val label: String)

enum class ProposalType { NEW_PLAN, CHANGE_PLAN, MULTI_PLAN, TARGET, WORKOUT_CHANGE, NEW_EXERCISE, NUTRITION_TARGET }

enum class ProposalState { PENDING, ACCEPTED, SUPERSEDED, UNDONE }

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

data class ProposedNutritionTarget(val kcal: Int?, val proteinG: Int?, val previousKcal: Int?, val previousProteinG: Int?, val reason: String?)

data class ProposedWorkoutChange(val sessionId: Long, val exerciseKeys: List<String>, val sets: List<PlannedSet>)

data class CoachProposal(
    val id: Long,
    val type: ProposalType,
    val state: ProposalState,
    val summary: String,
    val plans: List<ProposedPlan>,
    val target: ProposedTarget?,
    val workoutChange: ProposedWorkoutChange?,
    val newExercise: NewExercise?,
    val nutrition: ProposedNutritionTarget?,
    val createdAt: Instant,
    val isUndoable: Boolean,
)

data class WeeklyReviewSummary(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val workouts: Int,
    val plannedWorkouts: Int?,
    val records: Int,
    val weightFromKg: Double?,
    val weightToKg: Double?,
    val averageBalanceKcal: Int?,
    val averageProteinG: Int?,
    val averageSleepMinutes: Int?,
    val sleepChangeMinutes: Int?,
)

data class CoachMessage(
    val id: Long,
    val role: CoachRole,
    val text: String,
    val createdAt: Instant,
    val checked: List<String>,
    val proposals: List<CoachProposal>,
    val review: WeeklyReviewSummary?,
    val topic: CoachTopic?,
)

data class CoachNote(val id: Long, val text: String, val createdAt: Instant)

data class CoachUsage(val messages: Int, val inputTokens: Long, val outputTokens: Long)

data class CoachRun(
    val rounds: Int,
    val inputTokens: Int,
    val cacheReadTokens: Int,
    val cacheWriteTokens: Int,
    val outputTokens: Int,
    val duration: Duration,
    val stopReason: String?,
)
