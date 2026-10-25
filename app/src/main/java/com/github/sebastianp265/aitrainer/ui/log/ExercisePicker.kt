package com.github.sebastianp265.aitrainer.ui.log

import com.github.sebastianp265.aitrainer.domain.model.Exercise
import com.github.sebastianp265.aitrainer.domain.model.ExercisePreference
import java.text.Collator
import java.util.Locale

data class PickerItem(val exercise: Exercise, val displayName: String)

fun sortedPickerItems(exercises: List<Exercise>, displayName: (Exercise) -> String, locale: Locale): List<PickerItem> {
    val collator = Collator.getInstance(locale)
    return exercises.map { PickerItem(it, displayName(it)) }
        .sortedWith(
            compareBy<PickerItem> { it.exercise.preference == ExercisePreference.AVOID }
                .thenBy(collator) { it.displayName },
        )
}

fun List<PickerItem>.searchByDisplayOrEnglishName(query: String): List<PickerItem> {
    val q = query.trim()
    return filter { it.displayName.contains(q, ignoreCase = true) || it.exercise.name.contains(q, ignoreCase = true) }
}
