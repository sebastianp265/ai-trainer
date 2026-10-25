package com.github.sebastianp265.aitrainer.domain.model

enum class MuscleGroup {
    CHEST,
    LATS,
    UPPER_BACK,
    LOWER_BACK,
    TRAPS,
    FRONT_DELTS,
    SIDE_DELTS,
    REAR_DELTS,
    BICEPS,
    TRICEPS,
    FOREARMS,
    ABS,
    OBLIQUES,
    GLUTES,
    QUADS,
    HAMSTRINGS,
    ADDUCTORS,
    CALVES,
}

enum class Equipment {
    BARBELL,
    DUMBBELL,
    EZ_BAR,
    KETTLEBELL,
    CABLE,
    MACHINE,
    SMITH_MACHINE,
    BODYWEIGHT,
    PULL_UP_BAR,
    DIP_STATION,
    BAND,
}

enum class ExercisePreference { FAVOURITE, NEUTRAL, AVOID }

enum class Goal { CUT, MAINTAIN, BULK, RECOMP }

enum class EnergySource { POLAR, HEALTH_CONNECT, MANUAL }

enum class WeightSource { POLAR, MANUAL }
