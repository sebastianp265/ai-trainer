package com.github.sebastianp265.aitrainer.app

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.github.sebastianp265.aitrainer.core.db.TimeConverters
import com.github.sebastianp265.aitrainer.feature.exercise.data.ExerciseConverters
import com.github.sebastianp265.aitrainer.feature.exercise.data.ExerciseDao
import com.github.sebastianp265.aitrainer.feature.exercise.data.ExerciseEntity
import com.github.sebastianp265.aitrainer.feature.workout.data.SetEntryEntity
import com.github.sebastianp265.aitrainer.feature.workout.data.WorkoutDao
import com.github.sebastianp265.aitrainer.feature.workout.data.WorkoutSessionEntity

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutSessionEntity::class,
        SetEntryEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(TimeConverters::class, ExerciseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao

    companion object {
        const val NAME = "aitrainer.db"
    }
}
