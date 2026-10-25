package com.github.sebastianp265.aitrainer.ui.log

import com.github.sebastianp265.aitrainer.domain.model.Exercise
import com.github.sebastianp265.aitrainer.domain.model.ExercisePreference
import java.text.Collator
import java.util.Locale

data class PickerItem(val exercise: Exercise, val displayName: String)

fun sortedPickerItems(exercises: List<Exercise>, displayName: (Exercise) -> String, locale: Locale): List<PickerItem> {
    val collator = Collator.getInstance(locale)
    return exercises.map { PickerItem(it, displayName(it)) }.sortedWith(compareBy(collator) { it.displayName })
}

fun List<PickerItem>.withAvoidedLast(): List<PickerItem> = sortedBy { it.exercise.preference == ExercisePreference.AVOID }

fun List<PickerItem>.searchByDisplayOrEnglishName(query: String): List<PickerItem> {
    val q = query.trim()
    return filter { it.displayName.contains(q, ignoreCase = true) || it.exercise.name.contains(q, ignoreCase = true) }
}
