package com.github.sebastianp265.aitrainer.shared.database.impl

import com.github.sebastianp265.aitrainer.shared.nutrition.NutritionDao
import com.github.sebastianp265.aitrainer.shared.nutrition.NutritionTargetEntity
import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import com.github.sebastianp265.aitrainer.shared.coach.CoachDao
import com.github.sebastianp265.aitrainer.shared.database.DATABASE_SCHEMA_VERSION
import com.github.sebastianp265.aitrainer.shared.coach.CoachMessageEntity
import com.github.sebastianp265.aitrainer.shared.coach.CoachNoteEntity
import com.github.sebastianp265.aitrainer.shared.coach.CoachProfileEntity
import com.github.sebastianp265.aitrainer.shared.coach.CoachProposalEntity
import com.github.sebastianp265.aitrainer.shared.coach.CoachUsageEntity
import com.github.sebastianp265.aitrainer.shared.exercise.EquipmentEntity
import com.github.sebastianp265.aitrainer.shared.exercise.EquipmentTranslationEntity
import com.github.sebastianp265.aitrainer.shared.exercise.ExerciseEntity
import com.github.sebastianp265.aitrainer.shared.exercise.ExerciseMuscleEntity
import com.github.sebastianp265.aitrainer.shared.exercise.ExerciseTranslationEntity
import com.github.sebastianp265.aitrainer.shared.exercise.MuscleGroupEntity
import com.github.sebastianp265.aitrainer.shared.exercise.MuscleGroupTranslationEntity
import com.github.sebastianp265.aitrainer.shared.exercise.ExerciseDao
import com.github.sebastianp265.aitrainer.shared.plan.PlanDao
import com.github.sebastianp265.aitrainer.shared.plan.PlanEntity
import com.github.sebastianp265.aitrainer.shared.plan.PlannedSetEntity
import com.github.sebastianp265.aitrainer.shared.volume.VolumeDao
import com.github.sebastianp265.aitrainer.shared.volume.WeeklyTargetEntity
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
        PlanEntity::class,
        PlannedSetEntity::class,
        WeeklyTargetEntity::class,
        CoachMessageEntity::class,
        CoachProposalEntity::class,
        CoachNoteEntity::class,
        CoachProfileEntity::class,
        CoachUsageEntity::class,
        NutritionTargetEntity::class,
    ],
    version = DATABASE_SCHEMA_VERSION,
    autoMigrations = [
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 8, to = 9, spec = Migration8To9::class),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11),
        AutoMigration(from = 11, to = 12),
        AutoMigration(from = 12, to = 13, spec = Migration12To13::class),
        AutoMigration(from = 13, to = 14),
        AutoMigration(from = 14, to = 15),
        AutoMigration(from = 15, to = 16, spec = Migration15To16::class),
    ],
    exportSchema = true,
)
@TypeConverters(TimeConverters::class)
@ConstructedBy(AppDatabaseConstructor::class)
internal abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun planDao(): PlanDao
    abstract fun volumeDao(): VolumeDao
    abstract fun coachDao(): CoachDao
    abstract fun nutritionDao(): NutritionDao

    companion object {
        const val NAME = "aitrainer.db"
    }
}

internal expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
