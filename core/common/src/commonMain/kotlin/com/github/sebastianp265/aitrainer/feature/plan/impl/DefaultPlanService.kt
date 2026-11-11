package com.github.sebastianp265.aitrainer.feature.plan.impl

import com.github.sebastianp265.aitrainer.integration.plan.Plan
import com.github.sebastianp265.aitrainer.integration.plan.PlanAuthor
import com.github.sebastianp265.aitrainer.integration.plan.PlanDraft
import com.github.sebastianp265.aitrainer.integration.plan.PlanService
import com.github.sebastianp265.aitrainer.integration.plan.PlannedSet
import com.github.sebastianp265.aitrainer.shared.plan.PlanDao
import com.github.sebastianp265.aitrainer.shared.plan.PlanEntity
import com.github.sebastianp265.aitrainer.shared.plan.PlannedSetEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.time.Clock

internal class DefaultPlanService(
    private val dao: PlanDao,
    private val clock: Clock,
) : PlanService {

    override fun observeUpcoming(): Flow<List<Plan>> =
        combine(dao.observeUpcomingPlans(), dao.observeUpcomingPlannedSets()) { plans, sets ->
            val setsByPlan = sets.groupBy { it.planId }
            plans.map { it.toModel(setsByPlan[it.id].orEmpty()) }
        }

    override fun observePlan(id: Long): Flow<Plan?> =
        combine(dao.observePlan(id), dao.observePlannedSets(id)) { plan, sets -> plan?.toModel(sets) }

    override suspend fun createPlan(draft: PlanDraft, author: PlanAuthor): Long {
        require(draft.isSavable()) { "A plan needs a name and at least one set" }
        val id = dao.insertPlan(PlanEntity(name = draft.name.trim(), date = draft.date, author = author, createdAt = clock.now(), coachNote = draft.coachNote))
        dao.insertPlannedSets(draft.sets.toEntities(id))
        return id
    }

    override suspend fun updatePlan(id: Long, draft: PlanDraft) {
        require(draft.isSavable()) { "A plan needs a name and at least one set" }
        dao.updatePlan(id, draft.name.trim(), draft.date, draft.coachNote)
        dao.replacePlannedSets(id, draft.sets.toEntities(id))
    }

    override suspend fun deletePlan(id: Long) = dao.deletePlan(id)
}

private fun PlanDraft.isSavable() = name.isNotBlank() && sets.isNotEmpty() && sets.all { it.reps > 0 }

private fun List<PlannedSet>.toEntities(planId: Long) = mapIndexed { index, set ->
    PlannedSetEntity(
        planId = planId,
        exerciseKey = set.exerciseKey,
        positionInPlan = index,
        reps = set.reps,
        weightKg = set.weightKg,
        rpe = set.rpe,
        isWarmup = set.isWarmup,
    )
}

private fun PlanEntity.toModel(sets: List<PlannedSetEntity>) = Plan(
    id = id,
    name = name,
    date = date,
    author = author,
    sets = sets.sortedBy { it.positionInPlan }.map { PlannedSet(it.exerciseKey, it.reps, it.weightKg, it.rpe, it.isWarmup) },
    coachNote = coachNote,
)
