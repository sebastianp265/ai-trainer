package com.github.sebastianp265.aitrainer.data.repo

import com.github.sebastianp265.aitrainer.domain.model.Exercise
import com.github.sebastianp265.aitrainer.domain.model.SessionSummary
import com.github.sebastianp265.aitrainer.domain.model.SetEntry
import com.github.sebastianp265.aitrainer.domain.model.WorkoutSession
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun observeExercises(): Flow<List<Exercise>>

    suspend fun ensureSeeded()
}

interface WorkoutRepository {
    fun observeActiveSession(): Flow<WorkoutSession?>
    fun observeSets(sessionId: Long): Flow<List<SetEntry>>
    fun observeRecentSessions(limit: Int = 20): Flow<List<SessionSummary>>

    suspend fun startSession(planSessionRef: String? = null): Long
    suspend fun finishSession(sessionId: Long)
    suspend fun discardSession(sessionId: Long)

    suspend fun addSet(sessionId: Long, exerciseId: Long, reps: Int, weightKg: Double, isWarmup: Boolean = false): Long
    suspend fun modifySet(setId: Long, change: (SetEntry) -> SetEntry)
    suspend fun deleteSet(setId: Long)

    suspend fun lastSetsFor(exerciseId: Long, excludingSessionId: Long): List<SetEntry>
}
