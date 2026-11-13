package com.github.sebastianp265.aitrainer.feature.exercise.impl

import com.github.sebastianp265.aitrainer.integration.exercise.DEFAULT_EXERCISE_LANGUAGE
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import java.text.Collator
import java.util.Locale

internal fun Exercise.englishName(): String = namesByLanguage.getValue(DEFAULT_EXERCISE_LANGUAGE)

internal data class ExerciseListItem(val exercise: Exercise, val displayName: String, val englishName: String)

internal fun sortedExerciseListItems(
    exercises: List<Exercise>,
    displayName: (Exercise) -> String,
    englishName: (Exercise) -> String,
    locale: Locale,
): List<ExerciseListItem> {
    val collator = Collator.getInstance(locale)
    return exercises.map { ExerciseListItem(it, displayName(it), englishName(it)) }.sortedWith(compareBy(collator) { it.displayName })
}

internal fun List<ExerciseListItem>.searchByDisplayThenEnglishName(query: String): List<ExerciseListItem> {
    val q = query.trim()
    val (byDisplayName, others) = partition { it.displayName.contains(q, ignoreCase = true) }
    return byDisplayName + others.filter { it.englishName.contains(q, ignoreCase = true) }
}

internal val muscleGroupKeysInOrder = listOf("CHEST", "BACK", "LEGS", "SHOULDERS", "ARMS", "ABS")

internal fun muscleGroupOrder(key: String): Int = muscleGroupKeysInOrder.indexOf(key)

internal fun List<ExerciseListItem>.inPrimaryGroup(groupKey: String?): List<ExerciseListItem> =
    if (groupKey == null) this else filter { item -> item.exercise.primaryMuscles.any { it.key == groupKey } }
