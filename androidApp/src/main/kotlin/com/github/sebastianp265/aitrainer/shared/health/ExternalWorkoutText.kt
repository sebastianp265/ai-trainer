package com.github.sebastianp265.aitrainer.shared.health

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.integration.health.HealthWorkout
import com.github.sebastianp265.aitrainer.shared.localization.formatNumber

@Composable
internal fun externalWorkoutTitle(workout: HealthWorkout): String = stringResource(workoutTypeLabel(workout.type))

@Composable
internal fun externalWorkoutDetails(workout: HealthWorkout): String {
    val parts = listOfNotNull(
        stringResource(R.string.value_min, workout.durationMinutes.toString()),
        workout.distanceM?.let { stringResource(R.string.value_km, formatNumber(it / METERS_PER_KM, 1)) },
        workout.kcal?.let { stringResource(R.string.value_kcal, formatNumber(it.toDouble())) },
    )
    return parts.joinToString(" · ")
}

@StringRes
private fun workoutTypeLabel(type: String): Int = when (type) {
    "running", "running_treadmill" -> R.string.workout_type_running
    "biking", "biking_stationary" -> R.string.workout_type_biking
    "walking" -> R.string.workout_type_walking
    "hiking" -> R.string.workout_type_hiking
    "swimming_pool", "swimming_open_water" -> R.string.workout_type_swimming
    "rowing", "rowing_machine" -> R.string.workout_type_rowing
    "elliptical" -> R.string.workout_type_elliptical
    "yoga" -> R.string.workout_type_yoga
    "pilates" -> R.string.workout_type_pilates
    "stretching" -> R.string.workout_type_stretching
    "weightlifting", "strength_training" -> R.string.workout_type_strength
    "high_intensity_interval_training" -> R.string.workout_type_hiit
    else -> R.string.workout_type_other
}

private const val METERS_PER_KM = 1000.0
