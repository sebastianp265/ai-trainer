package com.github.sebastianp265.aitrainer.ui

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.data.db.SeedExercises
import com.github.sebastianp265.aitrainer.domain.model.Equipment
import com.github.sebastianp265.aitrainer.domain.model.Exercise
import com.github.sebastianp265.aitrainer.domain.model.ExercisePreference
import com.github.sebastianp265.aitrainer.domain.model.Goal
import com.github.sebastianp265.aitrainer.domain.model.MuscleGroup
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

fun Exercise.displayName(resources: Resources): String =
    SeedExercises.translatedNameRes[name]?.let(resources::getString) ?: name

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

@StringRes
fun Goal.labelRes(): Int = when (this) {
    Goal.CUT -> R.string.goal_cut
    Goal.MAINTAIN -> R.string.goal_maintain
    Goal.BULK -> R.string.goal_bulk
    Goal.RECOMP -> R.string.goal_recomp
}

@StringRes
fun Equipment.labelRes(): Int = when (this) {
    Equipment.BARBELL -> R.string.equipment_barbell
    Equipment.DUMBBELL -> R.string.equipment_dumbbell
    Equipment.EZ_BAR -> R.string.equipment_ez_bar
    Equipment.KETTLEBELL -> R.string.equipment_kettlebell
    Equipment.CABLE -> R.string.equipment_cable
    Equipment.MACHINE -> R.string.equipment_machine
    Equipment.SMITH_MACHINE -> R.string.equipment_smith_machine
    Equipment.BODYWEIGHT -> R.string.equipment_bodyweight
    Equipment.PULL_UP_BAR -> R.string.equipment_pull_up_bar
    Equipment.DIP_STATION -> R.string.equipment_dip_station
    Equipment.BAND -> R.string.equipment_band
}

@StringRes
fun ExercisePreference.labelRes(): Int = when (this) {
    ExercisePreference.FAVOURITE -> R.string.preference_favourite
    ExercisePreference.NEUTRAL -> R.string.preference_neutral
    ExercisePreference.AVOID -> R.string.preference_avoid
}

@Composable
@ReadOnlyComposable
private fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

@Composable
fun formatWeight(kg: Double): String {
    val locale = currentLocale()
    val format = remember(locale) { NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 2 } }
    return format.format(kg)
}

@Composable
fun formatDate(date: LocalDate): String {
    val locale = currentLocale()
    val format = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    return format.format(date.toJavaLocalDate())
}
