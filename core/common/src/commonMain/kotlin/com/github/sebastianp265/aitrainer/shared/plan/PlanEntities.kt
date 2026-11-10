package com.github.sebastianp265.aitrainer.shared.plan

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.github.sebastianp265.aitrainer.integration.plan.PlanAuthor
import com.github.sebastianp265.aitrainer.shared.exercise.ExerciseEntity
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

@Entity(tableName = "plan", indices = [Index("date")])
internal data class PlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val date: LocalDate?,
    val author: PlanAuthor,
    val createdAt: Instant,
    val coachNote: String? = null,
)

@Entity(
    tableName = "planned_set",
    foreignKeys = [
        ForeignKey(entity = PlanEntity::class, parentColumns = ["id"], childColumns = ["planId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ExerciseEntity::class, parentColumns = ["key"], childColumns = ["exerciseKey"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("planId"), Index("exerciseKey")],
)
internal data class PlannedSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val exerciseKey: String,
    val positionInPlan: Int,
    val reps: Int,
    val weightKg: Double,
    val rpe: Double?,
    val isWarmup: Boolean,
)
