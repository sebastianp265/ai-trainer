package com.github.sebastianp265.aitrainer.integration.workout

import com.github.sebastianp265.aitrainer.integration.plan.Plan
import com.github.sebastianp265.aitrainer.integration.plan.PlannedSet
import kotlinx.datetime.LocalDate

data class WorkoutReport(
    val session: WorkoutSession,
    val plan: Plan?,
    val durationMinutes: Long?,
    val workingSetCount: Int,
    val averageRpe: Double?,
    val exercises: List<ReportExercise>,
    val records: List<WorkoutRecord>,
    val previousWithSamePlan: PreviousWorkout?,
)

data class ReportExercise(
    val exerciseKey: String,
    val sets: List<SetEntry>,
    val groups: List<LastTimeGroup>,
    val plannedSets: List<PlannedSet>,
    val difference: PlanDifference?,
)

data class PlanDifference(
    val setCountDelta: Int,
    val weightDeltaKg: Double,
    val repsDelta: Int,
    val addedToPlan: Boolean,
)

enum class RecordKind { ONE_REP_MAX, REPS_AT_WEIGHT }

data class WorkoutRecord(
    val exerciseKey: String,
    val kind: RecordKind,
    val weightKg: Double,
    val reps: Int,
    val estimatedOneRepMaxKg: Double,
    val improvement: Double,
)

data class PreviousWorkout(
    val date: LocalDate,
    val name: String,
    val durationMinutes: Long?,
    val workingSetCount: Int,
    val averageRpe: Double?,
)
