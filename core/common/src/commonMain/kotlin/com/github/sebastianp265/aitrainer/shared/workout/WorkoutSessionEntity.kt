package com.github.sebastianp265.aitrainer.shared.workout

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.github.sebastianp265.aitrainer.shared.plan.PlanEntity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

@Entity(
    tableName = "workout_session",
    foreignKeys = [ForeignKey(entity = PlanEntity::class, parentColumns = ["id"], childColumns = ["planId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("date"), Index("planId")],
)
internal data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val startedAt: Instant,
    val finishedAt: Instant?,
    val planId: Long?,
    val notes: String?,
    val editedSessionId: Long? = null,
)
