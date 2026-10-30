package com.github.sebastianp265.aitrainer.data.db

import com.github.sebastianp265.aitrainer.data.db.entity.ExerciseEntity
import com.github.sebastianp265.aitrainer.domain.model.Equipment
import com.github.sebastianp265.aitrainer.domain.model.ExerciseKey
import com.github.sebastianp265.aitrainer.domain.model.Equipment.BAND
import com.github.sebastianp265.aitrainer.domain.model.Equipment.BARBELL
import com.github.sebastianp265.aitrainer.domain.model.Equipment.BODYWEIGHT
import com.github.sebastianp265.aitrainer.domain.model.Equipment.CABLE
import com.github.sebastianp265.aitrainer.domain.model.Equipment.DIP_STATION
import com.github.sebastianp265.aitrainer.domain.model.Equipment.DUMBBELL
import com.github.sebastianp265.aitrainer.domain.model.Equipment.EZ_BAR
import com.github.sebastianp265.aitrainer.domain.model.Equipment.KETTLEBELL
import com.github.sebastianp265.aitrainer.domain.model.Equipment.MACHINE
import com.github.sebastianp265.aitrainer.domain.model.Equipment.PULL_UP_BAR
import com.github.sebastianp265.aitrainer.domain.model.Equipment.SMITH_MACHINE
import com.github.sebastianp265.aitrainer.domain.model.MuscleGroup
import com.github.sebastianp265.aitrainer.domain.model.MuscleGroup.ABS
import com.github.sebastianp265.aitrainer.domain.model.MuscleGroup.ADDUCTORS
import com.github.sebastianp265.aitrainer.domain.model.MuscleGroup.BICEPS
import com.github.sebastianp265.aitrainer.domain.model.MuscleGroup.CALVES
import com.github.sebastianp265.aitrainer.domain.model.MuscleGroup.CHEST
import com.github.sebastianp265.aitrainer.domain.model.MuscleGroup.FOREARMS
import com.github.sebastianp265.aitrainer.domain.model.MuscleGroup.FRONT_DELTS
import com.github.sebastianp265.aitrainer.domain.model.MuscleGroup.GLUTES
import com.github.sebastianp265.aitrainer.domain.model.MuscleGroup.HAMSTRINGS
import com.github.sebastianp265.aitrainer.domain.model.MuscleGroup.LATS
import com.github.sebastianp265.aitrainer.domain.model.MuscleGroup.LOWER_BACK
import com.github.sebastianp265.aitrainer.domain.model.MuscleGroup.OBLIQUES
import com.github.sebastianp265.aitrainer.domain.model.MuscleGroup.QUADS
import com.github.sebastianp265.aitrainer.domain.model.MuscleGroup.REAR_DELTS
import com.github.sebastianp265.aitrainer.domain.model.MuscleGroup.SIDE_DELTS
import com.github.sebastianp265.aitrainer.domain.model.MuscleGroup.TRAPS
import com.github.sebastianp265.aitrainer.domain.model.MuscleGroup.TRICEPS
import com.github.sebastianp265.aitrainer.domain.model.MuscleGroup.UPPER_BACK

object ExerciseLibrary {

    private fun ex(
        key: ExerciseKey,
        equipment: Equipment,
        primary: List<MuscleGroup>,
        secondary: List<MuscleGroup> = emptyList(),
    ) = ExerciseEntity(key = key, primaryMuscles = primary, secondaryMuscles = secondary, equipment = equipment)

    private val chest = listOf(
        ex(ExerciseKey.BARBELL_BENCH_PRESS, BARBELL, listOf(CHEST), listOf(TRICEPS, FRONT_DELTS)),
        ex(ExerciseKey.INCLINE_BARBELL_BENCH_PRESS, BARBELL, listOf(CHEST, FRONT_DELTS), listOf(TRICEPS)),
        ex(ExerciseKey.DUMBBELL_BENCH_PRESS, DUMBBELL, listOf(CHEST), listOf(TRICEPS, FRONT_DELTS)),
        ex(ExerciseKey.INCLINE_DUMBBELL_PRESS, DUMBBELL, listOf(CHEST, FRONT_DELTS), listOf(TRICEPS)),
        ex(ExerciseKey.SMITH_MACHINE_INCLINE_PRESS, SMITH_MACHINE, listOf(CHEST, FRONT_DELTS), listOf(TRICEPS)),
        ex(ExerciseKey.MACHINE_CHEST_PRESS, MACHINE, listOf(CHEST), listOf(TRICEPS, FRONT_DELTS)),
        ex(ExerciseKey.CABLE_FLY, CABLE, listOf(CHEST)),
        ex(ExerciseKey.PEC_DECK, MACHINE, listOf(CHEST)),
        ex(ExerciseKey.PUSH_UP, BODYWEIGHT, listOf(CHEST), listOf(TRICEPS, FRONT_DELTS, ABS)),
        ex(ExerciseKey.DIP, DIP_STATION, listOf(CHEST, TRICEPS), listOf(FRONT_DELTS)),
    )

    private val back = listOf(
        ex(ExerciseKey.PULL_UP, PULL_UP_BAR, listOf(LATS), listOf(BICEPS, UPPER_BACK)),
        ex(ExerciseKey.CHIN_UP, PULL_UP_BAR, listOf(LATS, BICEPS), listOf(UPPER_BACK)),
        ex(ExerciseKey.LAT_PULLDOWN, CABLE, listOf(LATS), listOf(BICEPS, UPPER_BACK)),
        ex(ExerciseKey.BARBELL_ROW, BARBELL, listOf(UPPER_BACK, LATS), listOf(BICEPS, REAR_DELTS, LOWER_BACK)),
        ex(ExerciseKey.ONE_ARM_DUMBBELL_ROW, DUMBBELL, listOf(LATS, UPPER_BACK), listOf(BICEPS, REAR_DELTS)),
        ex(ExerciseKey.SEATED_CABLE_ROW, CABLE, listOf(UPPER_BACK, LATS), listOf(BICEPS, REAR_DELTS)),
        ex(ExerciseKey.CHEST_SUPPORTED_MACHINE_ROW, MACHINE, listOf(UPPER_BACK, LATS), listOf(BICEPS, REAR_DELTS)),
        ex(ExerciseKey.STRAIGHT_ARM_PULLDOWN, CABLE, listOf(LATS)),
        ex(ExerciseKey.BACK_EXTENSION, BODYWEIGHT, listOf(LOWER_BACK), listOf(GLUTES, HAMSTRINGS)),
        ex(ExerciseKey.BARBELL_SHRUG, BARBELL, listOf(TRAPS)),
        ex(ExerciseKey.DUMBBELL_SHRUG, DUMBBELL, listOf(TRAPS)),
    )

    private val shoulders = listOf(
        ex(ExerciseKey.OVERHEAD_PRESS, BARBELL, listOf(FRONT_DELTS), listOf(SIDE_DELTS, TRICEPS)),
        ex(ExerciseKey.SEATED_DUMBBELL_SHOULDER_PRESS, DUMBBELL, listOf(FRONT_DELTS), listOf(SIDE_DELTS, TRICEPS)),
        ex(ExerciseKey.MACHINE_SHOULDER_PRESS, MACHINE, listOf(FRONT_DELTS), listOf(SIDE_DELTS, TRICEPS)),
        ex(ExerciseKey.DUMBBELL_LATERAL_RAISE, DUMBBELL, listOf(SIDE_DELTS)),
        ex(ExerciseKey.CABLE_LATERAL_RAISE, CABLE, listOf(SIDE_DELTS)),
        ex(ExerciseKey.REAR_DELT_FLY, DUMBBELL, listOf(REAR_DELTS), listOf(UPPER_BACK)),
        ex(ExerciseKey.REVERSE_PEC_DECK, MACHINE, listOf(REAR_DELTS), listOf(UPPER_BACK)),
        ex(ExerciseKey.FACE_PULL, CABLE, listOf(REAR_DELTS), listOf(UPPER_BACK, TRAPS)),
        ex(ExerciseKey.BAND_PULL_APART, BAND, listOf(REAR_DELTS), listOf(UPPER_BACK)),
    )

    private val arms = listOf(
        ex(ExerciseKey.BARBELL_CURL, BARBELL, listOf(BICEPS), listOf(FOREARMS)),
        ex(ExerciseKey.EZ_BAR_CURL, EZ_BAR, listOf(BICEPS), listOf(FOREARMS)),
        ex(ExerciseKey.DUMBBELL_CURL, DUMBBELL, listOf(BICEPS), listOf(FOREARMS)),
        ex(ExerciseKey.HAMMER_CURL, DUMBBELL, listOf(BICEPS, FOREARMS)),
        ex(ExerciseKey.INCLINE_DUMBBELL_CURL, DUMBBELL, listOf(BICEPS)),
        ex(ExerciseKey.CABLE_CURL, CABLE, listOf(BICEPS)),
        ex(ExerciseKey.TRICEPS_PUSHDOWN, CABLE, listOf(TRICEPS)),
        ex(ExerciseKey.OVERHEAD_CABLE_TRICEPS_EXTENSION, CABLE, listOf(TRICEPS)),
        ex(ExerciseKey.EZ_BAR_SKULL_CRUSHER, EZ_BAR, listOf(TRICEPS)),
        ex(ExerciseKey.CLOSE_GRIP_BENCH_PRESS, BARBELL, listOf(TRICEPS), listOf(CHEST, FRONT_DELTS)),
        ex(ExerciseKey.WRIST_CURL, DUMBBELL, listOf(FOREARMS)),
    )

    private val legs = listOf(
        ex(ExerciseKey.BACK_SQUAT, BARBELL, listOf(QUADS, GLUTES), listOf(ADDUCTORS, LOWER_BACK)),
        ex(ExerciseKey.FRONT_SQUAT, BARBELL, listOf(QUADS), listOf(GLUTES, ABS)),
        ex(ExerciseKey.GOBLET_SQUAT, DUMBBELL, listOf(QUADS), listOf(GLUTES)),
        ex(ExerciseKey.LEG_PRESS, MACHINE, listOf(QUADS), listOf(GLUTES, ADDUCTORS)),
        ex(ExerciseKey.HACK_SQUAT, MACHINE, listOf(QUADS), listOf(GLUTES)),
        ex(ExerciseKey.BULGARIAN_SPLIT_SQUAT, DUMBBELL, listOf(QUADS, GLUTES), listOf(ADDUCTORS)),
        ex(ExerciseKey.WALKING_LUNGE, DUMBBELL, listOf(QUADS, GLUTES), listOf(HAMSTRINGS)),
        ex(ExerciseKey.LEG_EXTENSION, MACHINE, listOf(QUADS)),
        ex(ExerciseKey.CONVENTIONAL_DEADLIFT, BARBELL, listOf(HAMSTRINGS, GLUTES, LOWER_BACK), listOf(UPPER_BACK, TRAPS, FOREARMS, QUADS)),
        ex(ExerciseKey.ROMANIAN_DEADLIFT, BARBELL, listOf(HAMSTRINGS, GLUTES), listOf(LOWER_BACK)),
        ex(ExerciseKey.DUMBBELL_ROMANIAN_DEADLIFT, DUMBBELL, listOf(HAMSTRINGS, GLUTES), listOf(LOWER_BACK)),
        ex(ExerciseKey.LYING_LEG_CURL, MACHINE, listOf(HAMSTRINGS)),
        ex(ExerciseKey.SEATED_LEG_CURL, MACHINE, listOf(HAMSTRINGS)),
        ex(ExerciseKey.BARBELL_HIP_THRUST, BARBELL, listOf(GLUTES), listOf(HAMSTRINGS)),
        ex(ExerciseKey.KETTLEBELL_SWING, KETTLEBELL, listOf(GLUTES, HAMSTRINGS), listOf(LOWER_BACK)),
        ex(ExerciseKey.HIP_ADDUCTION_MACHINE, MACHINE, listOf(ADDUCTORS)),
        ex(ExerciseKey.STANDING_CALF_RAISE, MACHINE, listOf(CALVES)),
        ex(ExerciseKey.SEATED_CALF_RAISE, MACHINE, listOf(CALVES)),
    )

    private val core = listOf(
        ex(ExerciseKey.PLANK, BODYWEIGHT, listOf(ABS), listOf(OBLIQUES)),
        ex(ExerciseKey.HANGING_LEG_RAISE, PULL_UP_BAR, listOf(ABS), listOf(OBLIQUES)),
        ex(ExerciseKey.CABLE_CRUNCH, CABLE, listOf(ABS)),
        ex(ExerciseKey.AB_WHEEL_ROLLOUT, BODYWEIGHT, listOf(ABS), listOf(LATS)),
        ex(ExerciseKey.PALLOF_PRESS, CABLE, listOf(OBLIQUES), listOf(ABS)),
    )
    val all: List<ExerciseEntity> = chest + back + shoulders + arms + legs + core
}
