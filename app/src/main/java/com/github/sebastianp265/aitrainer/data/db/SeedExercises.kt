package com.github.sebastianp265.aitrainer.data.db

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

/** Inserted on every start, matched by name: renaming an entry creates a duplicate. */
object SeedExercises {

    private fun ex(
        name: String,
        equipment: Equipment,
        primary: List<MuscleGroup>,
        secondary: List<MuscleGroup> = emptyList(),
    ) = ExerciseEntity(
        name = name,
        primaryMuscles = primary,
        secondaryMuscles = secondary,
        equipment = equipment,
    )

    val all: List<ExerciseEntity> = listOf(
        // Chest
        ex("Barbell Bench Press", BARBELL, listOf(CHEST), listOf(TRICEPS, FRONT_DELTS)),
        ex("Incline Barbell Bench Press", BARBELL, listOf(CHEST, FRONT_DELTS), listOf(TRICEPS)),
        ex("Dumbbell Bench Press", DUMBBELL, listOf(CHEST), listOf(TRICEPS, FRONT_DELTS)),
        ex("Incline Dumbbell Press", DUMBBELL, listOf(CHEST, FRONT_DELTS), listOf(TRICEPS)),
        ex("Smith Machine Incline Press", SMITH_MACHINE, listOf(CHEST, FRONT_DELTS), listOf(TRICEPS)),
        ex("Machine Chest Press", MACHINE, listOf(CHEST), listOf(TRICEPS, FRONT_DELTS)),
        ex("Cable Fly", CABLE, listOf(CHEST)),
        ex("Pec Deck", MACHINE, listOf(CHEST)),
        ex("Push-Up", BODYWEIGHT, listOf(CHEST), listOf(TRICEPS, FRONT_DELTS, ABS)),
        ex("Dip", DIP_STATION, listOf(CHEST, TRICEPS), listOf(FRONT_DELTS)),

        // Back
        ex("Pull-Up", PULL_UP_BAR, listOf(LATS), listOf(BICEPS, UPPER_BACK)),
        ex("Chin-Up", PULL_UP_BAR, listOf(LATS, BICEPS), listOf(UPPER_BACK)),
        ex("Lat Pulldown", CABLE, listOf(LATS), listOf(BICEPS, UPPER_BACK)),
        ex("Barbell Row", BARBELL, listOf(UPPER_BACK, LATS), listOf(BICEPS, REAR_DELTS, LOWER_BACK)),
        ex("One-Arm Dumbbell Row", DUMBBELL, listOf(LATS, UPPER_BACK), listOf(BICEPS, REAR_DELTS)),
        ex("Seated Cable Row", CABLE, listOf(UPPER_BACK, LATS), listOf(BICEPS, REAR_DELTS)),
        ex("Chest-Supported Machine Row", MACHINE, listOf(UPPER_BACK, LATS), listOf(BICEPS, REAR_DELTS)),
        ex("Straight-Arm Pulldown", CABLE, listOf(LATS)),
        ex("Back Extension", BODYWEIGHT, listOf(LOWER_BACK), listOf(GLUTES, HAMSTRINGS)),
        ex("Barbell Shrug", BARBELL, listOf(TRAPS)),
        ex("Dumbbell Shrug", DUMBBELL, listOf(TRAPS)),

        // Shoulders
        ex("Overhead Press", BARBELL, listOf(FRONT_DELTS), listOf(SIDE_DELTS, TRICEPS)),
        ex("Seated Dumbbell Shoulder Press", DUMBBELL, listOf(FRONT_DELTS), listOf(SIDE_DELTS, TRICEPS)),
        ex("Machine Shoulder Press", MACHINE, listOf(FRONT_DELTS), listOf(SIDE_DELTS, TRICEPS)),
        ex("Dumbbell Lateral Raise", DUMBBELL, listOf(SIDE_DELTS)),
        ex("Cable Lateral Raise", CABLE, listOf(SIDE_DELTS)),
        ex("Rear Delt Fly", DUMBBELL, listOf(REAR_DELTS), listOf(UPPER_BACK)),
        ex("Reverse Pec Deck", MACHINE, listOf(REAR_DELTS), listOf(UPPER_BACK)),
        ex("Face Pull", CABLE, listOf(REAR_DELTS), listOf(UPPER_BACK, TRAPS)),
        ex("Band Pull-Apart", BAND, listOf(REAR_DELTS), listOf(UPPER_BACK)),

        // Arms
        ex("Barbell Curl", BARBELL, listOf(BICEPS), listOf(FOREARMS)),
        ex("EZ-Bar Curl", EZ_BAR, listOf(BICEPS), listOf(FOREARMS)),
        ex("Dumbbell Curl", DUMBBELL, listOf(BICEPS), listOf(FOREARMS)),
        ex("Hammer Curl", DUMBBELL, listOf(BICEPS, FOREARMS)),
        ex("Incline Dumbbell Curl", DUMBBELL, listOf(BICEPS)),
        ex("Cable Curl", CABLE, listOf(BICEPS)),
        ex("Triceps Pushdown", CABLE, listOf(TRICEPS)),
        ex("Overhead Cable Triceps Extension", CABLE, listOf(TRICEPS)),
        ex("EZ-Bar Skull Crusher", EZ_BAR, listOf(TRICEPS)),
        ex("Close-Grip Bench Press", BARBELL, listOf(TRICEPS), listOf(CHEST, FRONT_DELTS)),
        ex("Wrist Curl", DUMBBELL, listOf(FOREARMS)),

        // Legs
        ex("Back Squat", BARBELL, listOf(QUADS, GLUTES), listOf(ADDUCTORS, LOWER_BACK)),
        ex("Front Squat", BARBELL, listOf(QUADS), listOf(GLUTES, ABS)),
        ex("Goblet Squat", DUMBBELL, listOf(QUADS), listOf(GLUTES)),
        ex("Leg Press", MACHINE, listOf(QUADS), listOf(GLUTES, ADDUCTORS)),
        ex("Hack Squat", MACHINE, listOf(QUADS), listOf(GLUTES)),
        ex("Bulgarian Split Squat", DUMBBELL, listOf(QUADS, GLUTES), listOf(ADDUCTORS)),
        ex("Walking Lunge", DUMBBELL, listOf(QUADS, GLUTES), listOf(HAMSTRINGS)),
        ex("Leg Extension", MACHINE, listOf(QUADS)),
        ex("Conventional Deadlift", BARBELL, listOf(HAMSTRINGS, GLUTES, LOWER_BACK), listOf(UPPER_BACK, TRAPS, FOREARMS, QUADS)),
        ex("Romanian Deadlift", BARBELL, listOf(HAMSTRINGS, GLUTES), listOf(LOWER_BACK)),
        ex("Dumbbell Romanian Deadlift", DUMBBELL, listOf(HAMSTRINGS, GLUTES), listOf(LOWER_BACK)),
        ex("Lying Leg Curl", MACHINE, listOf(HAMSTRINGS)),
        ex("Seated Leg Curl", MACHINE, listOf(HAMSTRINGS)),
        ex("Barbell Hip Thrust", BARBELL, listOf(GLUTES), listOf(HAMSTRINGS)),
        ex("Kettlebell Swing", KETTLEBELL, listOf(GLUTES, HAMSTRINGS), listOf(LOWER_BACK)),
        ex("Hip Adduction Machine", MACHINE, listOf(ADDUCTORS)),
        ex("Standing Calf Raise", MACHINE, listOf(CALVES)),
        ex("Seated Calf Raise", MACHINE, listOf(CALVES)),

        // Core
        ex("Plank", BODYWEIGHT, listOf(ABS), listOf(OBLIQUES)),
        ex("Hanging Leg Raise", PULL_UP_BAR, listOf(ABS), listOf(OBLIQUES)),
        ex("Cable Crunch", CABLE, listOf(ABS)),
        ex("Ab Wheel Rollout", BODYWEIGHT, listOf(ABS), listOf(LATS)),
        ex("Pallof Press", CABLE, listOf(OBLIQUES), listOf(ABS)),
    )
}
