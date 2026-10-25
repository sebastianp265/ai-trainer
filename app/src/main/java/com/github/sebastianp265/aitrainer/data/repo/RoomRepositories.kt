package com.github.sebastianp265.aitrainer.data.repo

import com.github.sebastianp265.aitrainer.data.db.SeedExercises
import com.github.sebastianp265.aitrainer.data.db.dao.ExerciseDao
import com.github.sebastianp265.aitrainer.data.db.dao.SessionSummaryRow
import com.github.sebastianp265.aitrainer.data.db.dao.WorkoutDao
import com.github.sebastianp265.aitrainer.data.db.entity.ExerciseEntity
import com.github.sebastianp265.aitrainer.data.db.entity.SetEntryEntity
import com.github.sebastianp265.aitrainer.data.db.entity.WorkoutSessionEntity
import com.github.sebastianp265.aitrainer.domain.model.Exercise
import com.github.sebastianp265.aitrainer.domain.model.SessionSummary
import com.github.sebastianp265.aitrainer.domain.model.SetEntry
import com.github.sebastianp265.aitrainer.domain.model.WorkoutSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class RoomExerciseRepository(
    private val dao: ExerciseDao,
) : ExerciseRepository {
    override fun observeExercises(): Flow<List<Exercise>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun ensureSeeded() = dao.insertAllIgnoringExisting(SeedExercises.all)
}

class RoomWorkoutRepository(
    private val dao: WorkoutDao,
    private val clock: Clock,
) : WorkoutRepository {
    override fun observeActiveSession(): Flow<WorkoutSession?> =
        dao.observeActiveSession().map { it?.toDomain() }

    override fun observeSets(sessionId: Long): Flow<List<SetEntry>> =
        dao.observeSets(sessionId).map { list -> list.map { it.toDomain() } }

    override fun observeRecentSessions(limit: Int): Flow<List<SessionSummary>> =
        dao.observeRecentSessions(limit).map { list -> list.map { it.toDomain() } }

    override suspend fun startSession(planSessionRef: String?): Long =
        dao.insertSession(
            WorkoutSessionEntity(
                date = clock.todayIn(TimeZone.currentSystemDefault()),
                startedAt = clock.now(),
                finishedAt = null,
                planSessionRef = planSessionRef,
                notes = null,
            )
        )

    override suspend fun finishSession(sessionId: Long) {
        val session = dao.getSession(sessionId) ?: return
        dao.updateSession(session.copy(finishedAt = clock.now()))
    }

    override suspend fun discardSession(sessionId: Long) = dao.deleteSession(sessionId)

    override suspend fun addSet(
        sessionId: Long,
        exerciseId: Long,
        reps: Int,
        weightKg: Double,
        isWarmup: Boolean,
    ): Long = dao.insertSetAtEnd(
        SetEntryEntity(
            sessionId = sessionId,
            exerciseId = exerciseId,
            positionInSession = 0,
            reps = reps,
            weightKg = weightKg,
            rpe = null,
            isWarmup = isWarmup,
        )
    )

    override suspend fun modifySet(setId: Long, change: (SetEntry) -> SetEntry) =
        dao.modifySetInTransaction(setId) { change(it.toDomain()).toEntity() }

    override suspend fun deleteSet(setId: Long) = dao.deleteSet(setId)

    override suspend fun lastSetsFor(exerciseId: Long, excludingSessionId: Long): List<SetEntry> =
        dao.lastSetsForExercise(exerciseId, excludingSessionId).map { it.toDomain() }
}

internal fun ExerciseEntity.toDomain() = Exercise(id, name, primaryMuscles, secondaryMuscles, equipment, preference)

internal fun WorkoutSessionEntity.toDomain() = WorkoutSession(id, date, startedAt, finishedAt, planSessionRef, notes)

internal fun SetEntryEntity.toDomain() = SetEntry(id, sessionId, exerciseId, positionInSession, reps, weightKg, rpe, isWarmup)

internal fun SetEntry.toEntity() = SetEntryEntity(id, sessionId, exerciseId, positionInSession, reps, weightKg, rpe, isWarmup)

internal fun SessionSummaryRow.toDomain() = SessionSummary(id, date, startedAt, finishedAt, exerciseCount, setCount)
