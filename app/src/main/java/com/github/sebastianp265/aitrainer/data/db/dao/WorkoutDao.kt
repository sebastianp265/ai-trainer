package com.github.sebastianp265.aitrainer.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.github.sebastianp265.aitrainer.data.db.entity.SetEntryEntity
import com.github.sebastianp265.aitrainer.data.db.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

data class SessionSummaryRow(
    val id: Long,
    val date: LocalDate,
    val startedAt: Instant,
    val finishedAt: Instant?,
    val exerciseCount: Int,
    val setCount: Int,
)

@Dao
interface WorkoutDao {
    @Insert
    suspend fun insertSession(session: WorkoutSessionEntity): Long

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
               COUNT(DISTINCT e.exerciseId) AS exerciseCount,
               COUNT(e.id) AS setCount
        FROM workout_session s
        LEFT JOIN set_entry e ON e.sessionId = s.id
        WHERE s.finishedAt IS NOT NULL
        GROUP BY s.id
        ORDER BY s.startedAt DESC
        LIMIT :limit
        """
    )
    fun observeRecentSessions(limit: Int): Flow<List<SessionSummaryRow>>

    @Query("SELECT * FROM set_entry WHERE sessionId = :sessionId ORDER BY positionInSession")
    fun observeSets(sessionId: Long): Flow<List<SetEntryEntity>>

    @Query("SELECT COALESCE(MAX(positionInSession) + 1, 0) FROM set_entry WHERE sessionId = :sessionId")
    suspend fun nextSetOrder(sessionId: Long): Int

    @Insert
    suspend fun insertSet(set: SetEntryEntity): Long

    @Transaction
    suspend fun insertSetAtEnd(set: SetEntryEntity): Long =
        insertSet(set.copy(positionInSession = nextSetOrder(set.sessionId)))

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

    @Query(
        """
        SELECT * FROM set_entry
        WHERE exerciseId = :exerciseId AND sessionId = (
            SELECT s.id FROM workout_session s
            JOIN set_entry e ON e.sessionId = s.id
            WHERE e.exerciseId = :exerciseId AND s.id != :excludingSessionId
            ORDER BY s.startedAt DESC
            LIMIT 1
        )
        ORDER BY positionInSession
        """
    )
    suspend fun lastSetsForExercise(exerciseId: Long, excludingSessionId: Long): List<SetEntryEntity>
}
