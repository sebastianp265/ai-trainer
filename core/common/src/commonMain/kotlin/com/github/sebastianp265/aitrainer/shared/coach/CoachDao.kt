package com.github.sebastianp265.aitrainer.shared.coach

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.github.sebastianp265.aitrainer.integration.coach.ProposalState
import kotlinx.coroutines.flow.Flow

@Dao
internal interface CoachDao {
    @Query("SELECT * FROM coach_message ORDER BY id")
    fun observeMessages(): Flow<List<CoachMessageEntity>>

    @Query("SELECT * FROM coach_message ORDER BY id DESC LIMIT :limit")
    suspend fun lastMessages(limit: Int): List<CoachMessageEntity>

    @Insert
    suspend fun insertMessage(message: CoachMessageEntity): Long

    @Query("UPDATE coach_proposal SET messageId = :messageId WHERE id IN (:proposalIds)")
    suspend fun attachProposals(messageId: Long, proposalIds: List<Long>)

    @Query("DELETE FROM coach_message WHERE id = :id")
    suspend fun deleteMessage(id: Long)

    @Query("SELECT * FROM coach_proposal ORDER BY id")
    fun observeProposals(): Flow<List<CoachProposalEntity>>

    @Query("SELECT * FROM coach_proposal WHERE id = :id")
    suspend fun getProposal(id: Long): CoachProposalEntity?

    @Query("SELECT * FROM coach_proposal WHERE id = :id")
    fun observeProposal(id: Long): Flow<CoachProposalEntity?>

    @Query("SELECT * FROM coach_proposal WHERE state = :state AND subjectKey = :subjectKey")
    suspend fun proposalsInStateFor(state: ProposalState, subjectKey: String): List<CoachProposalEntity>

    @Insert
    suspend fun insertProposal(proposal: CoachProposalEntity): Long

    @Query("UPDATE coach_proposal SET state = :state WHERE id = :id")
    suspend fun setProposalState(id: Long, state: ProposalState)

    @Transaction
    suspend fun insertSupersedingProposal(proposal: CoachProposalEntity): Long {
        proposal.subjectKey?.let { key ->
            proposalsInStateFor(ProposalState.PENDING, key).forEach { setProposalState(it.id, ProposalState.SUPERSEDED) }
        }
        return insertProposal(proposal)
    }

    @Query("SELECT * FROM coach_note ORDER BY id")
    fun observeNotes(): Flow<List<CoachNoteEntity>>

    @Query("SELECT * FROM coach_note ORDER BY id")
    suspend fun notes(): List<CoachNoteEntity>

    @Insert
    suspend fun insertNote(note: CoachNoteEntity): Long

    @Query("UPDATE coach_note SET text = :text WHERE id = :id")
    suspend fun updateNote(id: Long, text: String): Int

    @Query("DELETE FROM coach_note WHERE id = :id")
    suspend fun deleteNote(id: Long): Int

    @Query("SELECT * FROM coach_profile")
    fun observeProfile(): Flow<List<CoachProfileEntity>>

    @Query("SELECT * FROM coach_profile")
    suspend fun profile(): List<CoachProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(entry: CoachProfileEntity)

    @Query("DELETE FROM coach_profile WHERE field = :field")
    suspend fun deleteProfileField(field: String)

    @Query("SELECT * FROM coach_usage WHERE month = :month")
    fun observeUsage(month: String): Flow<CoachUsageEntity?>

    @Query("SELECT * FROM coach_usage WHERE month = :month")
    suspend fun usage(month: String): CoachUsageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUsage(usage: CoachUsageEntity)

    @Query("SELECT * FROM coach_log ORDER BY id DESC LIMIT :limit")
    fun observeLog(limit: Int): Flow<List<CoachLogEntity>>

    @Insert
    suspend fun insertLog(entry: CoachLogEntity)

    @Query("DELETE FROM coach_log WHERE id NOT IN (SELECT id FROM coach_log ORDER BY id DESC LIMIT :keep)")
    suspend fun trimLog(keep: Int)

    @Query("DELETE FROM coach_log")
    suspend fun clearLog()
}
