package com.github.sebastianp265.aitrainer.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.github.sebastianp265.aitrainer.data.db.dao.BodyWeightDao
import com.github.sebastianp265.aitrainer.data.db.dao.DailyEnergyDao
import com.github.sebastianp265.aitrainer.data.db.dao.ExerciseDao
import com.github.sebastianp265.aitrainer.data.db.dao.PlanDao
import com.github.sebastianp265.aitrainer.data.db.dao.ProfilePrefsDao
import com.github.sebastianp265.aitrainer.data.db.dao.WorkoutDao
import com.github.sebastianp265.aitrainer.data.db.entity.BodyWeightEntity
import com.github.sebastianp265.aitrainer.data.db.entity.DailyEnergyEntity
import com.github.sebastianp265.aitrainer.data.db.entity.ExerciseEntity
import com.github.sebastianp265.aitrainer.data.db.entity.PlanEntity
import com.github.sebastianp265.aitrainer.data.db.entity.ProfilePrefsEntity
import com.github.sebastianp265.aitrainer.data.db.entity.SetEntryEntity
import com.github.sebastianp265.aitrainer.data.db.entity.WorkoutSessionEntity

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutSessionEntity::class,
        SetEntryEntity::class,
        DailyEnergyEntity::class,
        BodyWeightEntity::class,
        ProfilePrefsEntity::class,
        PlanEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun dailyEnergyDao(): DailyEnergyDao
    abstract fun bodyWeightDao(): BodyWeightDao
    abstract fun profilePrefsDao(): ProfilePrefsDao
    abstract fun planDao(): PlanDao

    companion object {
        const val NAME = "aitrainer.db"
    }
}
