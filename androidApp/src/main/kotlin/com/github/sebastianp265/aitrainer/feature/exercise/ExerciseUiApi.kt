package com.github.sebastianp265.aitrainer.feature.exercise

import com.github.sebastianp265.aitrainer.integration.exercise.DEFAULT_EXERCISE_LANGUAGE
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.integration.exercise.MuscleGroup
import java.util.Locale

internal fun Exercise.displayName(locale: Locale): String = namesByLanguage.localized(locale)

internal fun MuscleGroup.displayName(locale: Locale): String = namesByLanguage.localized(locale)

private fun Map<String, String>.localized(locale: Locale): String =
    this[locale.language] ?: getValue(DEFAULT_EXERCISE_LANGUAGE)
