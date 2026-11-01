package com.github.sebastianp265.aitrainer.shared.exercise

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "muscle_group")
internal data class MuscleGroupEntity(@PrimaryKey val key: String)

@Entity(
    tableName = "muscle_group_translation",
    primaryKeys = ["muscleGroupKey", "language"],
    foreignKeys = [ForeignKey(MuscleGroupEntity::class, ["key"], ["muscleGroupKey"], onDelete = ForeignKey.CASCADE)],
)
internal data class MuscleGroupTranslationEntity(val muscleGroupKey: String, val language: String, val name: String)

@Entity(tableName = "equipment")
internal data class EquipmentEntity(@PrimaryKey val key: String)

@Entity(
    tableName = "equipment_translation",
    primaryKeys = ["equipmentKey", "language"],
    foreignKeys = [ForeignKey(EquipmentEntity::class, ["key"], ["equipmentKey"], onDelete = ForeignKey.CASCADE)],
)
internal data class EquipmentTranslationEntity(val equipmentKey: String, val language: String, val name: String)

@Entity(
    tableName = "exercise",
    foreignKeys = [ForeignKey(EquipmentEntity::class, ["key"], ["equipmentKey"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("equipmentKey")],
)
internal data class ExerciseEntity(@PrimaryKey val key: String, val equipmentKey: String)

@Entity(
    tableName = "exercise_translation",
    primaryKeys = ["exerciseKey", "language"],
    foreignKeys = [ForeignKey(ExerciseEntity::class, ["key"], ["exerciseKey"], onDelete = ForeignKey.CASCADE)],
)
internal data class ExerciseTranslationEntity(val exerciseKey: String, val language: String, val name: String)

@Entity(
    tableName = "exercise_muscle",
    primaryKeys = ["exerciseKey", "muscleGroupKey"],
    foreignKeys = [
        ForeignKey(ExerciseEntity::class, ["key"], ["exerciseKey"], onDelete = ForeignKey.CASCADE),
        ForeignKey(MuscleGroupEntity::class, ["key"], ["muscleGroupKey"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("muscleGroupKey")],
)
internal data class ExerciseMuscleEntity(val exerciseKey: String, val muscleGroupKey: String, val isPrimary: Boolean)
