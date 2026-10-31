package com.github.sebastianp265.aitrainer.feature.exercise.ui

import android.content.res.Resources
import androidx.annotation.StringRes
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.feature.exercise.domain.model.Exercise
import com.github.sebastianp265.aitrainer.feature.exercise.domain.model.ExerciseKey
import com.github.sebastianp265.aitrainer.feature.exercise.domain.model.MuscleGroup

fun Exercise.displayName(resources: Resources): String = resources.getString(key.labelRes())

@StringRes
fun ExerciseKey.labelRes(): Int = when (this) {
    ExerciseKey.BARBELL_BENCH_PRESS -> R.string.exercise_barbell_bench_press
    ExerciseKey.INCLINE_BARBELL_BENCH_PRESS -> R.string.exercise_incline_barbell_bench_press
    ExerciseKey.DUMBBELL_BENCH_PRESS -> R.string.exercise_dumbbell_bench_press
    ExerciseKey.INCLINE_DUMBBELL_PRESS -> R.string.exercise_incline_dumbbell_press
    ExerciseKey.SMITH_MACHINE_INCLINE_PRESS -> R.string.exercise_smith_machine_incline_press
    ExerciseKey.MACHINE_CHEST_PRESS -> R.string.exercise_machine_chest_press
    ExerciseKey.CABLE_FLY -> R.string.exercise_cable_fly
    ExerciseKey.PEC_DECK -> R.string.exercise_pec_deck
    ExerciseKey.PUSH_UP -> R.string.exercise_push_up
    ExerciseKey.DIP -> R.string.exercise_dip
    ExerciseKey.PULL_UP -> R.string.exercise_pull_up
    ExerciseKey.CHIN_UP -> R.string.exercise_chin_up
    ExerciseKey.LAT_PULLDOWN -> R.string.exercise_lat_pulldown
    ExerciseKey.BARBELL_ROW -> R.string.exercise_barbell_row
    ExerciseKey.ONE_ARM_DUMBBELL_ROW -> R.string.exercise_one_arm_dumbbell_row
    ExerciseKey.SEATED_CABLE_ROW -> R.string.exercise_seated_cable_row
    ExerciseKey.CHEST_SUPPORTED_MACHINE_ROW -> R.string.exercise_chest_supported_machine_row
    ExerciseKey.STRAIGHT_ARM_PULLDOWN -> R.string.exercise_straight_arm_pulldown
    ExerciseKey.BACK_EXTENSION -> R.string.exercise_back_extension
    ExerciseKey.BARBELL_SHRUG -> R.string.exercise_barbell_shrug
    ExerciseKey.DUMBBELL_SHRUG -> R.string.exercise_dumbbell_shrug
    ExerciseKey.OVERHEAD_PRESS -> R.string.exercise_overhead_press
    ExerciseKey.SEATED_DUMBBELL_SHOULDER_PRESS -> R.string.exercise_seated_dumbbell_shoulder_press
    ExerciseKey.MACHINE_SHOULDER_PRESS -> R.string.exercise_machine_shoulder_press
    ExerciseKey.DUMBBELL_LATERAL_RAISE -> R.string.exercise_dumbbell_lateral_raise
    ExerciseKey.CABLE_LATERAL_RAISE -> R.string.exercise_cable_lateral_raise
    ExerciseKey.REAR_DELT_FLY -> R.string.exercise_rear_delt_fly
    ExerciseKey.REVERSE_PEC_DECK -> R.string.exercise_reverse_pec_deck
    ExerciseKey.FACE_PULL -> R.string.exercise_face_pull
    ExerciseKey.BAND_PULL_APART -> R.string.exercise_band_pull_apart
    ExerciseKey.BARBELL_CURL -> R.string.exercise_barbell_curl
    ExerciseKey.EZ_BAR_CURL -> R.string.exercise_ez_bar_curl
    ExerciseKey.DUMBBELL_CURL -> R.string.exercise_dumbbell_curl
    ExerciseKey.HAMMER_CURL -> R.string.exercise_hammer_curl
    ExerciseKey.INCLINE_DUMBBELL_CURL -> R.string.exercise_incline_dumbbell_curl
    ExerciseKey.CABLE_CURL -> R.string.exercise_cable_curl
    ExerciseKey.TRICEPS_PUSHDOWN -> R.string.exercise_triceps_pushdown
    ExerciseKey.OVERHEAD_CABLE_TRICEPS_EXTENSION -> R.string.exercise_overhead_cable_triceps_extension
    ExerciseKey.EZ_BAR_SKULL_CRUSHER -> R.string.exercise_ez_bar_skull_crusher
    ExerciseKey.CLOSE_GRIP_BENCH_PRESS -> R.string.exercise_close_grip_bench_press
    ExerciseKey.WRIST_CURL -> R.string.exercise_wrist_curl
    ExerciseKey.BACK_SQUAT -> R.string.exercise_back_squat
    ExerciseKey.FRONT_SQUAT -> R.string.exercise_front_squat
    ExerciseKey.GOBLET_SQUAT -> R.string.exercise_goblet_squat
    ExerciseKey.LEG_PRESS -> R.string.exercise_leg_press
    ExerciseKey.HACK_SQUAT -> R.string.exercise_hack_squat
    ExerciseKey.BULGARIAN_SPLIT_SQUAT -> R.string.exercise_bulgarian_split_squat
    ExerciseKey.WALKING_LUNGE -> R.string.exercise_walking_lunge
    ExerciseKey.LEG_EXTENSION -> R.string.exercise_leg_extension
    ExerciseKey.CONVENTIONAL_DEADLIFT -> R.string.exercise_conventional_deadlift
    ExerciseKey.ROMANIAN_DEADLIFT -> R.string.exercise_romanian_deadlift
    ExerciseKey.DUMBBELL_ROMANIAN_DEADLIFT -> R.string.exercise_dumbbell_romanian_deadlift
    ExerciseKey.LYING_LEG_CURL -> R.string.exercise_lying_leg_curl
    ExerciseKey.SEATED_LEG_CURL -> R.string.exercise_seated_leg_curl
    ExerciseKey.BARBELL_HIP_THRUST -> R.string.exercise_barbell_hip_thrust
    ExerciseKey.KETTLEBELL_SWING -> R.string.exercise_kettlebell_swing
    ExerciseKey.HIP_ADDUCTION_MACHINE -> R.string.exercise_hip_adduction_machine
    ExerciseKey.STANDING_CALF_RAISE -> R.string.exercise_standing_calf_raise
    ExerciseKey.SEATED_CALF_RAISE -> R.string.exercise_seated_calf_raise
    ExerciseKey.PLANK -> R.string.exercise_plank
    ExerciseKey.HANGING_LEG_RAISE -> R.string.exercise_hanging_leg_raise
    ExerciseKey.CABLE_CRUNCH -> R.string.exercise_cable_crunch
    ExerciseKey.AB_WHEEL_ROLLOUT -> R.string.exercise_ab_wheel_rollout
    ExerciseKey.PALLOF_PRESS -> R.string.exercise_pallof_press
}

@StringRes
fun MuscleGroup.labelRes(): Int = when (this) {
    MuscleGroup.CHEST -> R.string.muscle_chest
    MuscleGroup.LATS -> R.string.muscle_lats
    MuscleGroup.UPPER_BACK -> R.string.muscle_upper_back
    MuscleGroup.LOWER_BACK -> R.string.muscle_lower_back
    MuscleGroup.TRAPS -> R.string.muscle_traps
    MuscleGroup.FRONT_DELTS -> R.string.muscle_front_delts
    MuscleGroup.SIDE_DELTS -> R.string.muscle_side_delts
    MuscleGroup.REAR_DELTS -> R.string.muscle_rear_delts
    MuscleGroup.BICEPS -> R.string.muscle_biceps
    MuscleGroup.TRICEPS -> R.string.muscle_triceps
    MuscleGroup.FOREARMS -> R.string.muscle_forearms
    MuscleGroup.ABS -> R.string.muscle_abs
    MuscleGroup.OBLIQUES -> R.string.muscle_obliques
    MuscleGroup.GLUTES -> R.string.muscle_glutes
    MuscleGroup.QUADS -> R.string.muscle_quads
    MuscleGroup.HAMSTRINGS -> R.string.muscle_hamstrings
    MuscleGroup.ADDUCTORS -> R.string.muscle_adductors
    MuscleGroup.CALVES -> R.string.muscle_calves
}
