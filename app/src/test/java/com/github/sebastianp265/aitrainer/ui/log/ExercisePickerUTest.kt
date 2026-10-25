package com.github.sebastianp265.aitrainer.ui.log

import com.github.sebastianp265.aitrainer.domain.model.Equipment
import com.github.sebastianp265.aitrainer.domain.model.Exercise
import com.github.sebastianp265.aitrainer.domain.model.ExercisePreference
import com.github.sebastianp265.aitrainer.domain.model.MuscleGroup
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class ExercisePickerUTest {

    private val polish = Locale.forLanguageTag("pl")

    private val polishNames = mapOf(
        "Barbell Bench Press" to "Wyciskanie sztangi na ławce płaskiej",
        "Dumbbell Bench Press" to "Wyciskanie hantli na ławce płaskiej",
        "Barbell Shrug" to "Szrugsy ze sztangą",
        "Lat Pulldown" to "Ściąganie drążka wyciągu górnego",
        "Plank" to "Deska (plank)",
    )

    private fun exercise(id: Long, name: String, preference: ExercisePreference = ExercisePreference.NEUTRAL) =
        Exercise(id, name, listOf(MuscleGroup.CHEST), emptyList(), Equipment.BARBELL, preference)

    private val exercises = polishNames.keys.mapIndexed { i, name -> exercise(i.toLong(), name) }

    private fun polishItems(list: List<Exercise> = exercises) = sortedPickerItems(list, { polishNames.getValue(it.name) }, polish)

    @Test
    fun `English query finds exercises but shows their Polish names`() {
        val found = polishItems().searchByDisplayOrEnglishName("bench")

        assertEquals(
            listOf("Wyciskanie hantli na ławce płaskiej", "Wyciskanie sztangi na ławce płaskiej"),
            found.map { it.displayName },
        )
    }

    @Test
    fun `Polish query matches the displayed name, ignoring case and surrounding spaces`() {
        val found = polishItems().searchByDisplayOrEnglishName("  SZTANG ")

        assertEquals(
            listOf("Szrugsy ze sztangą", "Wyciskanie sztangi na ławce płaskiej"),
            found.map { it.displayName },
        )
    }

    @Test
    fun `sorted by Polish alphabet with avoided exercises last`() {
        val withAvoided = exercises.map {
            if (it.name == "Plank") it.copy(preference = ExercisePreference.AVOID) else it
        }

        assertEquals(
            listOf(
                "Szrugsy ze sztangą",
                "Ściąganie drążka wyciągu górnego",
                "Wyciskanie hantli na ławce płaskiej",
                "Wyciskanie sztangi na ławce płaskiej",
                "Deska (plank)",
            ),
            polishItems(withAvoided).searchByDisplayOrEnglishName("").map { it.displayName },
        )
    }
}
