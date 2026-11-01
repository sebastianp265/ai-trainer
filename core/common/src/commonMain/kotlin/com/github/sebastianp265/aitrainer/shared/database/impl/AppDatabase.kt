package com.github.sebastianp265.aitrainer.shared.database.impl

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import com.github.sebastianp265.aitrainer.shared.exercise.EquipmentEntity
import com.github.sebastianp265.aitrainer.shared.exercise.EquipmentTranslationEntity
import com.github.sebastianp265.aitrainer.shared.exercise.ExerciseEntity
import com.github.sebastianp265.aitrainer.shared.exercise.ExerciseMuscleEntity
import com.github.sebastianp265.aitrainer.shared.exercise.ExerciseTranslationEntity
import com.github.sebastianp265.aitrainer.shared.exercise.MuscleGroupEntity
import com.github.sebastianp265.aitrainer.shared.exercise.MuscleGroupTranslationEntity
import com.github.sebastianp265.aitrainer.shared.exercise.ExerciseDao
import com.github.sebastianp265.aitrainer.shared.workout.SetEntryEntity
import com.github.sebastianp265.aitrainer.shared.workout.WorkoutDao
import com.github.sebastianp265.aitrainer.shared.workout.WorkoutSessionEntity

@Database(
    entities = [
        MuscleGroupEntity::class,
        MuscleGroupTranslationEntity::class,
        EquipmentEntity::class,
        EquipmentTranslationEntity::class,
        ExerciseEntity::class,
        ExerciseTranslationEntity::class,
        ExerciseMuscleEntity::class,
        WorkoutSessionEntity::class,
        SetEntryEntity::class,
    ],
    version = 6,
    exportSchema = true,
)
@TypeConverters(TimeConverters::class)
@ConstructedBy(AppDatabaseConstructor::class)
internal abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao

    companion object {
        const val NAME = "aitrainer.db"
    }
}

@Suppress("KotlinNoActualForExpect")
internal expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
