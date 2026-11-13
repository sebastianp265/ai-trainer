package com.github.sebastianp265.aitrainer.integration.coach

import com.github.sebastianp265.aitrainer.integration.exercise.NewExercise
import com.github.sebastianp265.aitrainer.integration.plan.PlannedSet

interface ProposalDesk {
    suspend fun propose(draft: ProposalDraft): ProposalOutcome
}

sealed interface ProposalOutcome {
    data class Saved(val proposalId: Long) : ProposalOutcome

    data class Rejected(val reason: String) : ProposalOutcome
}

sealed interface ProposalDraft {
    val summary: String

    data class Plan(val plan: ProposedPlan, override val summary: String) : ProposalDraft

    data class Plans(val plans: List<ProposedPlan>, override val summary: String) : ProposalDraft

    data class Target(val groupKey: String, val minSets: Int, val maxSets: Int, val reason: String, override val summary: String) : ProposalDraft

    data class WorkoutChange(
        val sessionId: Long?,
        val sets: List<PlannedSet>,
        val removeExerciseKeys: List<String>,
        override val summary: String,
    ) : ProposalDraft

    data class Exercise(val exercise: NewExercise, override val summary: String) : ProposalDraft

    data class Nutrition(val kcal: Int?, val proteinG: Int?, val reason: String, override val summary: String) : ProposalDraft
}
