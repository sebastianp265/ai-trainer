package com.github.sebastianp265.aitrainer.shared.plan

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
internal interface PlanDao {
    @Query(
        """
        SELECT * FROM plan p
        WHERE NOT EXISTS (SELECT 1 FROM workout_session s WHERE s.planId = p.id AND s.finishedAt IS NOT NULL)
        ORDER BY p.date IS NULL, p.date, p.id
        """
    )
    fun observeUpcomingPlans(): Flow<List<PlanEntity>>

    @Query(
        """
        SELECT * FROM planned_set
        WHERE planId IN (
            SELECT p.id FROM plan p
            WHERE NOT EXISTS (SELECT 1 FROM workout_session s WHERE s.planId = p.id AND s.finishedAt IS NOT NULL)
        )
        ORDER BY planId, positionInPlan
        """
    )
    fun observeUpcomingPlannedSets(): Flow<List<PlannedSetEntity>>

    @Query("SELECT COUNT(*) FROM plan")
    fun observePlanCount(): Flow<Int>

    @Query("SELECT * FROM plan WHERE id = :id")
    fun observePlan(id: Long): Flow<PlanEntity?>

    @Query("SELECT * FROM plan WHERE id = :id")
    suspend fun getPlan(id: Long): PlanEntity?

    @Query("SELECT * FROM planned_set WHERE planId = :planId ORDER BY positionInPlan")
    fun observePlannedSets(planId: Long): Flow<List<PlannedSetEntity>>

    @Query("SELECT * FROM planned_set WHERE planId = :planId ORDER BY positionInPlan")
    suspend fun getPlannedSets(planId: Long): List<PlannedSetEntity>

    @Insert
    suspend fun insertPlan(plan: PlanEntity): Long

    @Insert
    suspend fun insertPlannedSets(sets: List<PlannedSetEntity>)

    @Query("UPDATE plan SET name = :name, date = :date WHERE id = :id")
    suspend fun updatePlan(id: Long, name: String, date: kotlinx.datetime.LocalDate?)

    @Query("DELETE FROM planned_set WHERE planId = :planId")
    suspend fun deletePlannedSets(planId: Long)

    @Query("DELETE FROM plan WHERE id = :id")
    suspend fun deletePlan(id: Long)

    @Transaction
    suspend fun replacePlannedSets(planId: Long, sets: List<PlannedSetEntity>) {
        deletePlannedSets(planId)
        insertPlannedSets(sets)
    }
}
