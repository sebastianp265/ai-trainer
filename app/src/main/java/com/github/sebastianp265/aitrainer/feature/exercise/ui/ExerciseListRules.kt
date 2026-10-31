package com.github.sebastianp265.aitrainer.feature.exercise.ui

import com.github.sebastianp265.aitrainer.feature.exercise.domain.model.Exercise
import java.text.Collator
import java.util.Locale

data class ExerciseListItem(val exercise: Exercise, val displayName: String, val englishName: String)

fun sortedExerciseListItems(
    exercises: List<Exercise>,
    displayName: (Exercise) -> String,
    englishName: (Exercise) -> String,
    locale: Locale,
): List<ExerciseListItem> {
    val collator = Collator.getInstance(locale)
    return exercises.map { ExerciseListItem(it, displayName(it), englishName(it)) }.sortedWith(compareBy(collator) { it.displayName })
}

fun List<ExerciseListItem>.searchByDisplayThenEnglishName(query: String): List<ExerciseListItem> {
    val q = query.trim()
    val (byDisplayName, others) = partition { it.displayName.contains(q, ignoreCase = true) }
    return byDisplayName + others.filter { it.englishName.contains(q, ignoreCase = true) }
}
