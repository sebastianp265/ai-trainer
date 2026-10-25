package com.github.sebastianp265.aitrainer.data.db

import androidx.annotation.StringRes
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.data.db.entity.ExerciseEntity
import com.github.sebastianp265.aitrainer.domain.model.Equipment
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

object SeedExercises {

    private class Seed(val entity: ExerciseEntity, @StringRes val translatedNameRes: Int)

    private fun ex(
        name: String,
        @StringRes nameRes: Int,
        equipment: Equipment,
        primary: List<MuscleGroup>,
        secondary: List<MuscleGroup> = emptyList(),
    ) = Seed(
        ExerciseEntity(
            name = name,
            primaryMuscles = primary,
            secondaryMuscles = secondary,
            equipment = equipment,
        ),
        nameRes,
    )

    private val chest = listOf(
        ex("Barbell Bench Press", R.string.exercise_barbell_bench_press, BARBELL, listOf(CHEST), listOf(TRICEPS, FRONT_DELTS)),
        ex("Incline Barbell Bench Press", R.string.exercise_incline_barbell_bench_press, BARBELL, listOf(CHEST, FRONT_DELTS), listOf(TRICEPS)),
        ex("Dumbbell Bench Press", R.string.exercise_dumbbell_bench_press, DUMBBELL, listOf(CHEST), listOf(TRICEPS, FRONT_DELTS)),
        ex("Incline Dumbbell Press", R.string.exercise_incline_dumbbell_press, DUMBBELL, listOf(CHEST, FRONT_DELTS), listOf(TRICEPS)),
        ex("Smith Machine Incline Press", R.string.exercise_smith_machine_incline_press, SMITH_MACHINE, listOf(CHEST, FRONT_DELTS), listOf(TRICEPS)),
        ex("Machine Chest Press", R.string.exercise_machine_chest_press, MACHINE, listOf(CHEST), listOf(TRICEPS, FRONT_DELTS)),
        ex("Cable Fly", R.string.exercise_cable_fly, CABLE, listOf(CHEST)),
        ex("Pec Deck", R.string.exercise_pec_deck, MACHINE, listOf(CHEST)),
        ex("Push-Up", R.string.exercise_push_up, BODYWEIGHT, listOf(CHEST), listOf(TRICEPS, FRONT_DELTS, ABS)),
        ex("Dip", R.string.exercise_dip, DIP_STATION, listOf(CHEST, TRICEPS), listOf(FRONT_DELTS)),
    )

    private val back = listOf(
        ex("Pull-Up", R.string.exercise_pull_up, PULL_UP_BAR, listOf(LATS), listOf(BICEPS, UPPER_BACK)),
        ex("Chin-Up", R.string.exercise_chin_up, PULL_UP_BAR, listOf(LATS, BICEPS), listOf(UPPER_BACK)),
        ex("Lat Pulldown", R.string.exercise_lat_pulldown, CABLE, listOf(LATS), listOf(BICEPS, UPPER_BACK)),
        ex("Barbell Row", R.string.exercise_barbell_row, BARBELL, listOf(UPPER_BACK, LATS), listOf(BICEPS, REAR_DELTS, LOWER_BACK)),
        ex("One-Arm Dumbbell Row", R.string.exercise_one_arm_dumbbell_row, DUMBBELL, listOf(LATS, UPPER_BACK), listOf(BICEPS, REAR_DELTS)),
        ex("Seated Cable Row", R.string.exercise_seated_cable_row, CABLE, listOf(UPPER_BACK, LATS), listOf(BICEPS, REAR_DELTS)),
        ex("Chest-Supported Machine Row", R.string.exercise_chest_supported_machine_row, MACHINE, listOf(UPPER_BACK, LATS), listOf(BICEPS, REAR_DELTS)),
        ex("Straight-Arm Pulldown", R.string.exercise_straight_arm_pulldown, CABLE, listOf(LATS)),
        ex("Back Extension", R.string.exercise_back_extension, BODYWEIGHT, listOf(LOWER_BACK), listOf(GLUTES, HAMSTRINGS)),
        ex("Barbell Shrug", R.string.exercise_barbell_shrug, BARBELL, listOf(TRAPS)),
        ex("Dumbbell Shrug", R.string.exercise_dumbbell_shrug, DUMBBELL, listOf(TRAPS)),
    )

    private val shoulders = listOf(
        ex("Overhead Press", R.string.exercise_overhead_press, BARBELL, listOf(FRONT_DELTS), listOf(SIDE_DELTS, TRICEPS)),
        ex("Seated Dumbbell Shoulder Press", R.string.exercise_seated_dumbbell_shoulder_press, DUMBBELL, listOf(FRONT_DELTS), listOf(SIDE_DELTS, TRICEPS)),
        ex("Machine Shoulder Press", R.string.exercise_machine_shoulder_press, MACHINE, listOf(FRONT_DELTS), listOf(SIDE_DELTS, TRICEPS)),
        ex("Dumbbell Lateral Raise", R.string.exercise_dumbbell_lateral_raise, DUMBBELL, listOf(SIDE_DELTS)),
        ex("Cable Lateral Raise", R.string.exercise_cable_lateral_raise, CABLE, listOf(SIDE_DELTS)),
        ex("Rear Delt Fly", R.string.exercise_rear_delt_fly, DUMBBELL, listOf(REAR_DELTS), listOf(UPPER_BACK)),
        ex("Reverse Pec Deck", R.string.exercise_reverse_pec_deck, MACHINE, listOf(REAR_DELTS), listOf(UPPER_BACK)),
        ex("Face Pull", R.string.exercise_face_pull, CABLE, listOf(REAR_DELTS), listOf(UPPER_BACK, TRAPS)),
        ex("Band Pull-Apart", R.string.exercise_band_pull_apart, BAND, listOf(REAR_DELTS), listOf(UPPER_BACK)),
    )

    private val arms = listOf(
        ex("Barbell Curl", R.string.exercise_barbell_curl, BARBELL, listOf(BICEPS), listOf(FOREARMS)),
        ex("EZ-Bar Curl", R.string.exercise_ez_bar_curl, EZ_BAR, listOf(BICEPS), listOf(FOREARMS)),
        ex("Dumbbell Curl", R.string.exercise_dumbbell_curl, DUMBBELL, listOf(BICEPS), listOf(FOREARMS)),
        ex("Hammer Curl", R.string.exercise_hammer_curl, DUMBBELL, listOf(BICEPS, FOREARMS)),
        ex("Incline Dumbbell Curl", R.string.exercise_incline_dumbbell_curl, DUMBBELL, listOf(BICEPS)),
        ex("Cable Curl", R.string.exercise_cable_curl, CABLE, listOf(BICEPS)),
        ex("Triceps Pushdown", R.string.exercise_triceps_pushdown, CABLE, listOf(TRICEPS)),
        ex("Overhead Cable Triceps Extension", R.string.exercise_overhead_cable_triceps_extension, CABLE, listOf(TRICEPS)),
        ex("EZ-Bar Skull Crusher", R.string.exercise_ez_bar_skull_crusher, EZ_BAR, listOf(TRICEPS)),
        ex("Close-Grip Bench Press", R.string.exercise_close_grip_bench_press, BARBELL, listOf(TRICEPS), listOf(CHEST, FRONT_DELTS)),
        ex("Wrist Curl", R.string.exercise_wrist_curl, DUMBBELL, listOf(FOREARMS)),
    )

    private val legs = listOf(
        ex("Back Squat", R.string.exercise_back_squat, BARBELL, listOf(QUADS, GLUTES), listOf(ADDUCTORS, LOWER_BACK)),
        ex("Front Squat", R.string.exercise_front_squat, BARBELL, listOf(QUADS), listOf(GLUTES, ABS)),
        ex("Goblet Squat", R.string.exercise_goblet_squat, DUMBBELL, listOf(QUADS), listOf(GLUTES)),
        ex("Leg Press", R.string.exercise_leg_press, MACHINE, listOf(QUADS), listOf(GLUTES, ADDUCTORS)),
        ex("Hack Squat", R.string.exercise_hack_squat, MACHINE, listOf(QUADS), listOf(GLUTES)),
        ex("Bulgarian Split Squat", R.string.exercise_bulgarian_split_squat, DUMBBELL, listOf(QUADS, GLUTES), listOf(ADDUCTORS)),
        ex("Walking Lunge", R.string.exercise_walking_lunge, DUMBBELL, listOf(QUADS, GLUTES), listOf(HAMSTRINGS)),
        ex("Leg Extension", R.string.exercise_leg_extension, MACHINE, listOf(QUADS)),
        ex("Conventional Deadlift", R.string.exercise_conventional_deadlift, BARBELL, listOf(HAMSTRINGS, GLUTES, LOWER_BACK), listOf(UPPER_BACK, TRAPS, FOREARMS, QUADS)),
        ex("Romanian Deadlift", R.string.exercise_romanian_deadlift, BARBELL, listOf(HAMSTRINGS, GLUTES), listOf(LOWER_BACK)),
        ex("Dumbbell Romanian Deadlift", R.string.exercise_dumbbell_romanian_deadlift, DUMBBELL, listOf(HAMSTRINGS, GLUTES), listOf(LOWER_BACK)),
        ex("Lying Leg Curl", R.string.exercise_lying_leg_curl, MACHINE, listOf(HAMSTRINGS)),
        ex("Seated Leg Curl", R.string.exercise_seated_leg_curl, MACHINE, listOf(HAMSTRINGS)),
        ex("Barbell Hip Thrust", R.string.exercise_barbell_hip_thrust, BARBELL, listOf(GLUTES), listOf(HAMSTRINGS)),
        ex("Kettlebell Swing", R.string.exercise_kettlebell_swing, KETTLEBELL, listOf(GLUTES, HAMSTRINGS), listOf(LOWER_BACK)),
        ex("Hip Adduction Machine", R.string.exercise_hip_adduction_machine, MACHINE, listOf(ADDUCTORS)),
        ex("Standing Calf Raise", R.string.exercise_standing_calf_raise, MACHINE, listOf(CALVES)),
        ex("Seated Calf Raise", R.string.exercise_seated_calf_raise, MACHINE, listOf(CALVES)),
    )

    private val core = listOf(
        ex("Plank", R.string.exercise_plank, BODYWEIGHT, listOf(ABS), listOf(OBLIQUES)),
        ex("Hanging Leg Raise", R.string.exercise_hanging_leg_raise, PULL_UP_BAR, listOf(ABS), listOf(OBLIQUES)),
        ex("Cable Crunch", R.string.exercise_cable_crunch, CABLE, listOf(ABS)),
        ex("Ab Wheel Rollout", R.string.exercise_ab_wheel_rollout, BODYWEIGHT, listOf(ABS), listOf(LATS)),
        ex("Pallof Press", R.string.exercise_pallof_press, CABLE, listOf(OBLIQUES), listOf(ABS)),
    )

    private val seeds = chest + back + shoulders + arms + legs + core

    val all: List<ExerciseEntity> = seeds.map { it.entity }

    val translatedNameRes: Map<String, Int> = seeds.associate { it.entity.name to it.translatedNameRes }
}
