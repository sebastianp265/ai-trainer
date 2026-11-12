package com.github.sebastianp265.aitrainer.shared.workout

import com.github.sebastianp265.aitrainer.integration.workout.RPE_MAX
import com.github.sebastianp265.aitrainer.integration.workout.RPE_MIN
import com.github.sebastianp265.aitrainer.integration.workout.RPE_STEP

internal val rpeScale: List<Double> = generateSequence(RPE_MIN) { it + RPE_STEP }.takeWhile { it <= RPE_MAX }.toList()
