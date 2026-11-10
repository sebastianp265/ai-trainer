package com.github.sebastianp265.aitrainer.feature.workout.impl

import com.github.sebastianp265.aitrainer.shared.plan.PlanDao
import com.github.sebastianp265.aitrainer.shared.workout.SessionSummaryRow
import com.github.sebastianp265.aitrainer.shared.workout.SetEntryEntity
import com.github.sebastianp265.aitrainer.shared.workout.WorkoutDao
import com.github.sebastianp265.aitrainer.shared.workout.WorkoutSessionEntity
import com.github.sebastianp265.aitrainer.integration.workout.CopyResult
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.integration.workout.ExerciseBlock
import com.github.sebastianp265.aitrainer.integration.workout.ExerciseHistory
import com.github.sebastianp265.aitrainer.integration.workout.ExerciseStrength
import com.github.sebastianp265.aitrainer.integration.workout.LastTimeGroup
import com.github.sebastianp265.aitrainer.integration.workout.RecentExercise
import com.github.sebastianp265.aitrainer.integration.workout.SessionSummary
import com.github.sebastianp265.aitrainer.integration.workout.SetChange
import com.github.sebastianp265.aitrainer.integration.workout.SetEntry
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutService
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

internal class DefaultWorkoutService(
    private val dao: WorkoutDao,
    private val planDao: PlanDao,
    private val clock: Clock,
) : WorkoutService {

    private companion object {
        const val RECENT_SESSIONS_LIMIT = 20
        const val NO_SESSION = -1L
    }

    override fun observeActiveSession(): Flow<WorkoutSession?> =
        dao.observeActiveSession().map { it?.toModel() }

    override fun observeSets(sessionId: Long): Flow<List<SetEntry>> =
        dao.observeSets(sessionId).map { list -> list.map { it.toModel() } }

    override fun observeRecentSessions(): Flow<List<SessionSummary>> =
        dao.observeRecentSessions(RECENT_SESSIONS_LIMIT).map { list -> list.map { it.toModel() } }

    override fun observeFinishedSessions(from: LocalDate, toInclusive: LocalDate): Flow<List<SessionSummary>> =
        dao.observeFinishedSessions(from, toInclusive).map { list -> list.map { it.toModel() } }

    override fun observeStrength(from: LocalDate, toInclusive: LocalDate): Flow<List<ExerciseStrength>> =
        dao.observeWorkingSets().map { sets -> strengthInRange(sets, from, toInclusive) }

    override fun observeExerciseHistory(exerciseKey: String): Flow<ExerciseHistory> =
        dao.observeHistorySets(exerciseKey).map { exerciseHistory(exerciseKey, it, clock.todayIn(TimeZone.currentSystemDefault())) }

    override fun groupSetsByExercise(sets: List<SetEntry>, exercises: Map<String, Exercise>): List<ExerciseBlock> =
        groupSetsByExerciseInLoggingOrder(sets, exercises)

    override fun groupLastTimeSets(sets: List<SetEntry>): List<LastTimeGroup> = lastTimeGroups(sets)

    override suspend fun lastWorkingSets(exerciseKey: String, excludingSessionId: Long): List<SetEntry> =
        lastSetsFor(exerciseKey, excludingSessionId).filter { !it.isWarmup }

    override suspend fun recentExercises(limit: Int): List<RecentExercise> =
        dao.recentExerciseKeys(limit).map { key ->
            RecentExercise(key, lastTimeGroups(lastWorkingSets(key, excludingSessionId = NO_SESSION)))
        }

    override suspend fun startSession(planId: Long?) {
        val plannedSets = planId?.let { planDao.getPlannedSets(it) }.orEmpty()
        dao.insertSessionWithSets(
            WorkoutSessionEntity(
                date = clock.todayIn(TimeZone.currentSystemDefault()),
                startedAt = clock.now(),
                finishedAt = null,
                planId = planId,
                notes = null,
            ),
            plannedSets.map {
                SetEntryEntity(
                    sessionId = 0,
                    exerciseKey = it.exerciseKey,
                    positionInSession = it.positionInPlan,
                    reps = it.reps,
                    weightKg = it.weightKg,
                    rpe = it.rpe,
                    isWarmup = it.isWarmup,
                )
            },
        )
    }

    override suspend fun finishSession(sessionId: Long) {
        val session = dao.getSession(sessionId) ?: return
        dao.updateSession(session.copy(finishedAt = clock.now()))
    }

    override suspend fun discardSession(sessionId: Long) = dao.deleteSession(sessionId)

    override suspend fun addExercise(sessionId: Long, exerciseKey: String) {
        val lastWorkingSet = lastSetsFor(exerciseKey, excludingSessionId = sessionId).firstOrNull { !it.isWarmup }
        insertSet(
            sessionId = sessionId,
            exerciseKey = exerciseKey,
            reps = lastWorkingSet?.reps ?: DEFAULT_REPS,
            weightKg = lastWorkingSet?.weightKg ?: DEFAULT_WEIGHT_KG,
        )
    }

    override suspend fun addSet(sessionId: Long, block: ExerciseBlock) {
        val previousSet = block.sets.lastOrNull()
        insertSet(
            sessionId = sessionId,
            exerciseKey = block.exercise.key,
            reps = previousSet?.reps ?: DEFAULT_REPS,
            weightKg = previousSet?.weightKg ?: DEFAULT_WEIGHT_KG,
        )
    }

    override suspend fun copySetsFromLastTime(sessionId: Long, exerciseKey: String): CopyResult {
        val previousSets = lastSetsFor(exerciseKey, excludingSessionId = sessionId)
        if (previousSets.isEmpty()) return CopyResult.NO_PREVIOUS_SETS
        previousSets.forEach { insertSet(sessionId, it.exerciseKey, it.reps, it.weightKg, it.isWarmup) }
        return CopyResult.COPIED
    }

    override suspend fun changeSet(setId: Long, change: SetChange) {
        dao.modifySetInTransaction(setId) { it.toModel().apply(change).toEntity() }
    }

    override suspend fun deleteSet(setId: Long) = dao.deleteSet(setId)

    override suspend fun removeExercise(sessionId: Long, exerciseKey: String) =
        dao.deleteExerciseSets(sessionId, exerciseKey)

    private suspend fun lastSetsFor(exerciseKey: String, excludingSessionId: Long): List<SetEntry> =
        dao.lastSetsForExercise(exerciseKey, excludingSessionId).map { it.toModel() }

    private suspend fun insertSet(
        sessionId: Long,
        exerciseKey: String,
        reps: Int,
        weightKg: Double,
        isWarmup: Boolean = false,
    ): Long = dao.insertSetAtEnd(
        SetEntryEntity(
            sessionId = sessionId,
            exerciseKey = exerciseKey,
            positionInSession = 0,
            reps = reps,
            weightKg = weightKg,
            rpe = null,
            isWarmup = isWarmup,
        )
    )

    private fun SetEntry.apply(change: SetChange): SetEntry = when (change) {
        is SetChange.Reps -> copy(reps = adjustReps(reps, change.delta))
        is SetChange.RepsTo -> copy(reps = change.reps.coerceAtLeast(0))
        is SetChange.WeightTo -> copy(weightKg = change.kg.coerceAtLeast(0.0))
        is SetChange.RpeTo -> copy(rpe = change.rpe)
        SetChange.ToggleWarmup -> copy(isWarmup = !isWarmup)
    }
}

private fun SessionSummaryRow.toModel() = SessionSummary(id, date, startedAt, finishedAt, exerciseCount, setCount)

private fun WorkoutSessionEntity.toModel() = WorkoutSession(id, date, startedAt, finishedAt, planId, notes)

private fun SetEntryEntity.toModel() = SetEntry(id, sessionId, exerciseKey, positionInSession, reps, weightKg, rpe, isWarmup)

private fun SetEntry.toEntity() = SetEntryEntity(id, sessionId, exerciseKey, positionInSession, reps, weightKg, rpe, isWarmup)
