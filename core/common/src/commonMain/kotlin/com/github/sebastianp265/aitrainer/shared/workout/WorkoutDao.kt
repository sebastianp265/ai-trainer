package com.github.sebastianp265.aitrainer.shared.workout

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

@Dao
internal interface WorkoutDao {
    @Insert
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Transaction
    suspend fun insertSessionWithSets(session: WorkoutSessionEntity, sets: List<SetEntryEntity>): Long {
        val sessionId = insertSession(session)
        sets.forEach { insertSet(it.copy(sessionId = sessionId)) }
        return sessionId
    }

    @Update
    suspend fun updateSession(session: WorkoutSessionEntity)

    @Query("SELECT * FROM workout_session WHERE id = :id")
    suspend fun getSession(id: Long): WorkoutSessionEntity?

    @Query("DELETE FROM workout_session WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("SELECT * FROM workout_session WHERE finishedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeActiveSession(): Flow<WorkoutSessionEntity?>

    @Query(
        """
        SELECT s.id, s.date, s.startedAt, s.finishedAt,
               COUNT(DISTINCT e.exerciseKey) AS exerciseCount,
               COUNT(CASE WHEN e.isWarmup = 0 THEN e.id END) AS setCount,
               s.planId AS planId, p.name AS planName
        FROM workout_session s
        LEFT JOIN set_entry e ON e.sessionId = s.id
        LEFT JOIN plan p ON p.id = s.planId
        WHERE s.finishedAt IS NOT NULL
        GROUP BY s.id
        ORDER BY s.startedAt DESC
        LIMIT :limit
        """
    )
    fun observeRecentSessions(limit: Int): Flow<List<SessionSummaryRow>>

    @Query(
        """
        SELECT s.id, s.date, s.startedAt, s.finishedAt,
               COUNT(DISTINCT e.exerciseKey) AS exerciseCount,
               COUNT(CASE WHEN e.isWarmup = 0 THEN e.id END) AS setCount,
               s.planId AS planId, p.name AS planName
        FROM workout_session s
        LEFT JOIN set_entry e ON e.sessionId = s.id
        LEFT JOIN plan p ON p.id = s.planId
        WHERE s.finishedAt IS NOT NULL AND s.date BETWEEN :from AND :toInclusive
        GROUP BY s.id
        ORDER BY s.startedAt DESC
        """
    )
    fun observeFinishedSessions(from: LocalDate, toInclusive: LocalDate): Flow<List<SessionSummaryRow>>

    @Query(
        """
        SELECT s.date AS date, e.exerciseKey AS exerciseKey, e.weightKg AS weightKg, e.reps AS reps
        FROM set_entry e
        JOIN workout_session s ON s.id = e.sessionId
        WHERE s.finishedAt IS NOT NULL AND e.isWarmup = 0
        """
    )
    fun observeWorkingSets(): Flow<List<WorkingSetRow>>

    @Query(
        """
        SELECT e.exerciseKey FROM set_entry e
        JOIN workout_session s ON s.id = e.sessionId
        WHERE s.finishedAt IS NOT NULL
        GROUP BY e.exerciseKey
        ORDER BY MAX(s.startedAt) DESC
        LIMIT :limit
        """
    )
    suspend fun recentExerciseKeys(limit: Int): List<String>

    @Query(
        """
        SELECT s.id AS sessionId, s.date AS date, p.name AS planName, e.positionInSession AS positionInSession,
               e.reps AS reps, e.weightKg AS weightKg, e.rpe AS rpe
        FROM set_entry e
        JOIN workout_session s ON s.id = e.sessionId
        LEFT JOIN plan p ON p.id = s.planId
        WHERE e.exerciseKey = :exerciseKey AND s.finishedAt IS NOT NULL AND e.isWarmup = 0
        ORDER BY s.startedAt, e.positionInSession
        """
    )
    fun observeHistorySets(exerciseKey: String): Flow<List<HistorySetRow>>

    @Query("SELECT * FROM set_entry WHERE sessionId = :sessionId ORDER BY positionInSession")
    fun observeSets(sessionId: Long): Flow<List<SetEntryEntity>>

    @Query("SELECT COALESCE(MAX(positionInSession) + 1, 0) FROM set_entry WHERE sessionId = :sessionId")
    suspend fun nextSetOrder(sessionId: Long): Int

    @Insert
    suspend fun insertSet(set: SetEntryEntity): Long

    @Insert
    suspend fun insertSets(sets: List<SetEntryEntity>)

    @Query("DELETE FROM set_entry WHERE sessionId = :sessionId")
    suspend fun deleteSessionSets(sessionId: Long)

    @Transaction
    suspend fun replaceSessionSets(sessionId: Long, sets: List<SetEntryEntity>) {
        deleteSessionSets(sessionId)
        insertSets(sets)
    }

    @Transaction
    suspend fun moveSetsAndDeleteSession(fromSessionId: Long, toSessionId: Long) {
        replaceSessionSets(toSessionId, getSets(fromSessionId).map { it.copy(id = 0, sessionId = toSessionId) })
        deleteSession(fromSessionId)
    }

    @Transaction
    suspend fun insertSetAtEnd(set: SetEntryEntity): Long =
        insertSet(set.copy(positionInSession = nextSetOrder(set.sessionId)))

    @Query("SELECT * FROM set_entry WHERE sessionId = :sessionId ORDER BY positionInSession")
    suspend fun getSets(sessionId: Long): List<SetEntryEntity>

    @Query(
        """
        SELECT s.date AS date, e.exerciseKey AS exerciseKey, e.weightKg AS weightKg, e.reps AS reps
        FROM set_entry e
        JOIN workout_session s ON s.id = e.sessionId
        WHERE s.finishedAt IS NOT NULL AND s.startedAt < :before AND e.isWarmup = 0 AND e.exerciseKey IN (:exerciseKeys)
        """
    )
    suspend fun workingSetsBefore(before: Instant, exerciseKeys: List<String>): List<WorkingSetRow>

    @Query(
        """
        SELECT s.* FROM workout_session s
        JOIN plan p ON p.id = s.planId
        WHERE p.name = :planName AND s.finishedAt IS NOT NULL AND s.startedAt < :before
        ORDER BY s.startedAt DESC
        LIMIT 1
        """
    )
    suspend fun previousSessionOfPlanNamed(planName: String, before: Instant): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_session WHERE finishedAt IS NULL LIMIT 1")
    suspend fun getActiveSession(): WorkoutSessionEntity?

    @Query("SELECT * FROM set_entry WHERE id = :id")
    suspend fun getSet(id: Long): SetEntryEntity?

    @Update
    suspend fun updateSet(set: SetEntryEntity)

    @Transaction
    suspend fun modifySetInTransaction(id: Long, change: (SetEntryEntity) -> SetEntryEntity) {
        getSet(id)?.let { updateSet(change(it)) }
    }

    @Query("DELETE FROM set_entry WHERE id = :id")
    suspend fun deleteSet(id: Long)

    @Query("DELETE FROM set_entry WHERE sessionId = :sessionId AND exerciseKey = :exerciseKey")
    suspend fun deleteExerciseSets(sessionId: Long, exerciseKey: String)

    @Query(
        """
        SELECT * FROM set_entry
        WHERE exerciseKey = :exerciseKey AND sessionId = (
            SELECT s.id FROM workout_session s
            JOIN set_entry e ON e.sessionId = s.id
            WHERE e.exerciseKey = :exerciseKey AND s.id NOT IN (:excludedSessionIds) AND s.finishedAt IS NOT NULL
            ORDER BY s.startedAt DESC
            LIMIT 1
        )
        ORDER BY positionInSession
        """
    )
    suspend fun lastSetsForExercise(exerciseKey: String, excludedSessionIds: List<Long>): List<SetEntryEntity>
}
