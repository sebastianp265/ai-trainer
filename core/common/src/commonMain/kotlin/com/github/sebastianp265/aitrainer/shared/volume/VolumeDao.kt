package com.github.sebastianp265.aitrainer.shared.volume

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Dao
internal interface VolumeDao {
    @Query(
        """
        SELECT s.date AS date, m.muscleGroupKey AS groupKey, m.isPrimary AS isPrimary
        FROM set_entry e
        JOIN workout_session s ON s.id = e.sessionId
        JOIN exercise_muscle m ON m.exerciseKey = e.exerciseKey
        WHERE s.finishedAt IS NOT NULL AND e.isWarmup = 0 AND s.date BETWEEN :from AND :toInclusive
        """
    )
    fun observeDoneSets(from: LocalDate, toInclusive: LocalDate): Flow<List<MuscleSetRow>>

    @Query(
        """
        SELECT p.date AS date, m.muscleGroupKey AS groupKey, m.isPrimary AS isPrimary
        FROM planned_set ps
        JOIN plan p ON p.id = ps.planId
        JOIN exercise_muscle m ON m.exerciseKey = ps.exerciseKey
        WHERE ps.isWarmup = 0 AND p.date BETWEEN :from AND :toInclusive
          AND NOT EXISTS (SELECT 1 FROM workout_session s WHERE s.planId = p.id AND s.finishedAt IS NOT NULL)
        """
    )
    fun observePlannedSets(from: LocalDate, toInclusive: LocalDate): Flow<List<MuscleSetRow>>

    @Query("SELECT * FROM weekly_target")
    fun observeTargets(): Flow<List<WeeklyTargetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTarget(target: WeeklyTargetEntity)

    @Query("DELETE FROM weekly_target")
    suspend fun deleteAllTargets()
}
