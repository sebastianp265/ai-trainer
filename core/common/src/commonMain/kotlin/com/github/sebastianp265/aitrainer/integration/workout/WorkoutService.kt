package com.github.sebastianp265.aitrainer.integration.workout

import com.github.sebastianp265.aitrainer.integration.plan.PlannedSet

import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface WorkoutService {
    fun observeActiveSession(): Flow<WorkoutSession?>

    fun observeSets(sessionId: Long): Flow<List<SetEntry>>

    fun observeRecentSessions(): Flow<List<SessionSummary>>

    fun observeFinishedSessions(from: LocalDate, toInclusive: LocalDate): Flow<List<SessionSummary>>

    fun observeStrength(from: LocalDate, toInclusive: LocalDate): Flow<List<ExerciseStrength>>

    fun observeWorkoutReport(sessionId: Long): Flow<WorkoutReport?>

    fun observeExerciseHistory(exerciseKey: String): Flow<ExerciseHistory>

    fun groupSetsByExercise(sets: List<SetEntry>, exercises: Map<String, Exercise>): List<ExerciseBlock>

    fun groupLastTimeSets(sets: List<SetEntry>): List<LastTimeGroup>

    suspend fun lastWorkingSets(exerciseKey: String, excludingSessionId: Long): List<SetEntry>

    suspend fun recentExercises(limit: Int): List<RecentExercise>

    suspend fun startSession(planId: Long? = null)

    suspend fun finishSession(sessionId: Long)

    suspend fun discardSession(sessionId: Long)

    suspend fun reopenSession(sessionId: Long): Boolean

    suspend fun addExercise(sessionId: Long, exerciseKey: String)

    suspend fun addSet(sessionId: Long, block: ExerciseBlock)

    suspend fun copySetsFromLastTime(sessionId: Long, exerciseKey: String): CopyResult

    suspend fun changeSet(setId: Long, change: SetChange)

    suspend fun deleteSet(setId: Long)

    suspend fun removeExercise(sessionId: Long, exerciseKey: String)

    suspend fun findSession(sessionId: Long): WorkoutSession?

    suspend fun addSetWith(sessionId: Long, set: PlannedSet)

    suspend fun replaceExerciseSets(sessionId: Long, exerciseKeys: List<String>, sets: List<PlannedSet>)
}
