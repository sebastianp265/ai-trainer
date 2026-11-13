package com.github.sebastianp265.aitrainer.integration.plan

import kotlinx.coroutines.flow.Flow

interface PlanService {
    fun observeUpcoming(): Flow<List<Plan>>

    fun observePlan(id: Long): Flow<Plan?>

    suspend fun createPlan(draft: PlanDraft, author: PlanAuthor): Long

    suspend fun updatePlan(id: Long, draft: PlanDraft)

    suspend fun deletePlan(id: Long)

    suspend fun movePlan(id: Long, toIndex: Int)
}
