package com.github.sebastianp265.aitrainer.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.github.sebastianp265.aitrainer.domain.model.EnergySource
import com.github.sebastianp265.aitrainer.domain.model.Equipment
import com.github.sebastianp265.aitrainer.domain.model.ExercisePreference
import com.github.sebastianp265.aitrainer.domain.model.Goal
import com.github.sebastianp265.aitrainer.domain.model.MuscleGroup
import com.github.sebastianp265.aitrainer.domain.model.WeightSource
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

@Entity(tableName = "exercise", indices = [Index(value = ["name"], unique = true)])
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val primaryMuscles: List<MuscleGroup>,
    val secondaryMuscles: List<MuscleGroup>,
    val equipment: Equipment,
    val preference: ExercisePreference = ExercisePreference.NEUTRAL,
)

@Entity(tableName = "workout_session", indices = [Index("date")])
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val startedAt: Instant,
    val finishedAt: Instant?,
    val planSessionRef: String?,
    val notes: String?,
)

@Entity(
    tableName = "set_entry",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("sessionId"), Index("exerciseId")],
)
data class SetEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    // "order" is an SQL keyword.
    @ColumnInfo(name = "setOrder") val order: Int,
    val reps: Int,
    val weightKg: Double,
    val rpe: Double?,
    val isWarmup: Boolean,
)

@Entity(tableName = "daily_energy")
data class DailyEnergyEntity(
    @PrimaryKey val date: LocalDate,
    val kcalBurned: Int?,
    val kcalBurnedSource: EnergySource?,
    val kcalEaten: Int?,
    val proteinG: Double?,
    val carbsG: Double?,
    val fatG: Double?,
    val updatedAt: Instant,
)

@Entity(
    tableName = "body_weight",
    indices = [Index(value = ["measuredAt", "source"], unique = true)],
)
data class BodyWeightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val measuredAt: Instant,
    val weightKg: Double,
    val source: WeightSource,
)

@Entity(tableName = "profile_prefs")
data class ProfilePrefsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val goal: Goal,
    val sessionsPerWeek: Int,
    val sessionLengthMin: Int,
    val weakMuscleGroups: List<MuscleGroup>,
    val availableEquipment: List<Equipment>,
    val notes: String,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

@Entity(tableName = "plan", indices = [Index("isActive")])
data class PlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val version: Int,
    val createdAt: Instant,
    val isActive: Boolean,
    val validFrom: LocalDate,
    val validTo: LocalDate,
    val json: String,
    val rationale: String,
)
