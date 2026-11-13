package com.github.sebastianp265.aitrainer.shared.coach

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.github.sebastianp265.aitrainer.integration.coach.CoachRole
import com.github.sebastianp265.aitrainer.integration.coach.ProposalState
import com.github.sebastianp265.aitrainer.integration.coach.ProposalType
import com.github.sebastianp265.aitrainer.integration.coach.TopicKind
import kotlin.time.Instant

@Entity(tableName = "coach_message")
internal data class CoachMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: CoachRole,
    val text: String,
    val createdAt: Instant,
    val checked: String?,
    val review: String?,
    @Embedded(prefix = "topic_") val topic: CoachTopicColumns? = null,
)

internal data class CoachTopicColumns(val kind: TopicKind, val id: String, val label: String)

@Entity(tableName = "coach_proposal")
internal data class CoachProposalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: ProposalType,
    val state: ProposalState,
    val summary: String,
    val payload: String,
    val subjectKey: String?,
    val messageId: Long?,
    val createdAt: Instant,
    val undo: String? = null,
)

@Entity(tableName = "coach_note")
internal data class CoachNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val createdAt: Instant,
)

@Entity(tableName = "coach_profile")
internal data class CoachProfileEntity(
    @PrimaryKey val field: String,
    val value: String,
)
