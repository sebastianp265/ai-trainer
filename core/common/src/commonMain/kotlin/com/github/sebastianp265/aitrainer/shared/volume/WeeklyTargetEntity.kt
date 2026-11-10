package com.github.sebastianp265.aitrainer.shared.volume

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.github.sebastianp265.aitrainer.integration.volume.TargetSource
import com.github.sebastianp265.aitrainer.shared.exercise.MuscleGroupEntity
import kotlinx.datetime.LocalDate

@Entity(
    tableName = "weekly_target",
    foreignKeys = [ForeignKey(entity = MuscleGroupEntity::class, parentColumns = ["key"], childColumns = ["muscleGroupKey"], onDelete = ForeignKey.CASCADE)],
)
internal data class WeeklyTargetEntity(
    @PrimaryKey val muscleGroupKey: String,
    val minSets: Int,
    val maxSets: Int,
    val source: TargetSource,
    val setOn: LocalDate,
    val reason: String?,
)
