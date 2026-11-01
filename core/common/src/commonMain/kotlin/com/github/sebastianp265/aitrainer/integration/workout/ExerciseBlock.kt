package com.github.sebastianp265.aitrainer.integration.workout

import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
data class ExerciseBlock(
    val exercise: Exercise,
    val sets: List<SetEntry>,
)
