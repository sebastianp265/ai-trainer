package com.github.sebastianp265.aitrainer.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.github.sebastianp265.aitrainer.data.db.dao.ExerciseDao
import com.github.sebastianp265.aitrainer.data.db.dao.ProfilePrefsDao
import com.github.sebastianp265.aitrainer.data.db.dao.WorkoutDao
import com.github.sebastianp265.aitrainer.data.db.entity.ExerciseEntity
import com.github.sebastianp265.aitrainer.data.db.entity.ProfilePrefsEntity
import com.github.sebastianp265.aitrainer.data.db.entity.SetEntryEntity
import com.github.sebastianp265.aitrainer.data.db.entity.WorkoutSessionEntity

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutSessionEntity::class,
        SetEntryEntity::class,
        ProfilePrefsEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun profilePrefsDao(): ProfilePrefsDao

    companion object {
        const val NAME = "aitrainer.db"
    }
}
