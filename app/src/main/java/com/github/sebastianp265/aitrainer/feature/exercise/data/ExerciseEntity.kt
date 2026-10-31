package com.github.sebastianp265.aitrainer.feature.exercise.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.github.sebastianp265.aitrainer.feature.exercise.domain.model.Equipment
import com.github.sebastianp265.aitrainer.feature.exercise.domain.model.ExerciseKey
import com.github.sebastianp265.aitrainer.feature.exercise.domain.model.MuscleGroup

@Entity(tableName = "exercise", indices = [Index(value = ["key"], unique = true)])
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: ExerciseKey,
    val primaryMuscles: List<MuscleGroup>,
    val secondaryMuscles: List<MuscleGroup>,
    val equipment: Equipment,
)
