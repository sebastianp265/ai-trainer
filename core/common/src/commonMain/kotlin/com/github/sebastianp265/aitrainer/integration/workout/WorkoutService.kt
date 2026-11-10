package com.github.sebastianp265.aitrainer.integration.workout

import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface WorkoutService {
    fun observeActiveSession(): Flow<WorkoutSession?>

    fun observeSets(sessionId: Long): Flow<List<SetEntry>>

    fun observeRecentSessions(): Flow<List<SessionSummary>>

    fun observeFinishedSessions(from: LocalDate, toInclusive: LocalDate): Flow<List<SessionSummary>>

    fun observeStrength(from: LocalDate, toInclusive: LocalDate): Flow<List<ExerciseStrength>>

    fun groupSetsByExercise(sets: List<SetEntry>, exercises: Map<String, Exercise>): List<ExerciseBlock>

    fun groupLastTimeSets(sets: List<SetEntry>): List<LastTimeGroup>

    suspend fun lastWorkingSets(exerciseKey: String, excludingSessionId: Long): List<SetEntry>

    suspend fun startSession()

    suspend fun finishSession(sessionId: Long)

    suspend fun discardSession(sessionId: Long)

    suspend fun addExercise(sessionId: Long, exerciseKey: String)

    suspend fun addSet(sessionId: Long, block: ExerciseBlock)

    suspend fun copySetsFromLastTime(sessionId: Long, exerciseKey: String): CopyResult

    suspend fun changeSet(setId: Long, change: SetChange)

    suspend fun deleteSet(setId: Long)

    suspend fun removeExercise(sessionId: Long, exerciseKey: String)
}
