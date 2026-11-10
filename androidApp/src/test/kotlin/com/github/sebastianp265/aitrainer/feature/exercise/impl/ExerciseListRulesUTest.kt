package com.github.sebastianp265.aitrainer.feature.exercise.impl

import com.github.sebastianp265.aitrainer.integration.exercise.Equipment
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class ExerciseListRulesUTest {

    private val polish = Locale.forLanguageTag("pl")

    private val polishNames = mapOf(
        "BARBELL_BENCH_PRESS" to "Wyciskanie sztangi na ławce płaskiej",
        "DUMBBELL_BENCH_PRESS" to "Wyciskanie hantli na ławce płaskiej",
        "BARBELL_SHRUG" to "Szrugsy ze sztangą",
        "LAT_PULLDOWN" to "Ściąganie drążka wyciągu górnego",
        "PLANK" to "Deska (plank)",
        "PEC_DECK" to "Rozpiętki na maszynie (butterfly)",
        "CABLE_FLY" to "Rozpiętki na wyciągu",
        "REAR_DELT_FLY" to "Odwrotne rozpiętki z hantlami",
    )

    private val englishNames = mapOf(
        "BARBELL_BENCH_PRESS" to "Barbell Bench Press",
        "DUMBBELL_BENCH_PRESS" to "Dumbbell Bench Press",
        "BARBELL_SHRUG" to "Barbell Shrug",
        "LAT_PULLDOWN" to "Lat Pulldown",
        "PLANK" to "Plank",
        "PEC_DECK" to "Pec Deck",
        "CABLE_FLY" to "Cable Fly",
        "REAR_DELT_FLY" to "Rear Delt Fly",
    )

    private val exercises = polishNames.keys.map { key ->
        Exercise(
            key = key,
            namesByLanguage = mapOf("pl" to polishNames.getValue(key), "en" to englishNames.getValue(key)),
            equipment = Equipment("BODYWEIGHT", mapOf("en" to "Bodyweight")),
            primaryMuscles = emptyList(),
            secondaryMuscles = emptyList(),
        )
    }

    private fun polishItems() =
        sortedExerciseListItems(exercises, { polishNames.getValue(it.key) }, { englishNames.getValue(it.key) }, polish)

    @Test
    fun `query matches the displayed name, ignoring case and surrounding spaces`() {
        val found = polishItems().searchByDisplayThenEnglishName("  SZTANG ")

        assertEquals(
            listOf("Szrugsy ze sztangą", "Wyciskanie sztangi na ławce płaskiej"),
            found.map { it.displayName },
        )
    }

    @Test
    fun `English query finds exercises but shows their Polish names`() {
        val found = polishItems().searchByDisplayThenEnglishName("bench")

        assertEquals(
            listOf("Wyciskanie hantli na ławce płaskiej", "Wyciskanie sztangi na ławce płaskiej"),
            found.map { it.displayName },
        )
    }

    @Test
    fun `matches by the displayed name come before matches by the English name`() {
        val found = polishItems().searchByDisplayThenEnglishName("fly")

        assertEquals(
            listOf("Rozpiętki na maszynie (butterfly)", "Odwrotne rozpiętki z hantlami", "Rozpiętki na wyciągu"),
            found.map { it.displayName },
        )
    }

    @Test
    fun `sorted by Polish alphabet`() {
        assertEquals(
            listOf(
                "Deska (plank)",
                "Odwrotne rozpiętki z hantlami",
                "Rozpiętki na maszynie (butterfly)",
                "Rozpiętki na wyciągu",
                "Szrugsy ze sztangą",
                "Ściąganie drążka wyciągu górnego",
                "Wyciskanie hantli na ławce płaskiej",
                "Wyciskanie sztangi na ławce płaskiej",
            ),
            polishItems().map { it.displayName },
        )
    }

    @Test
    fun `group filter keeps exercises whose primary group matches and nothing else`() {
        val legs = com.github.sebastianp265.aitrainer.integration.exercise.MuscleGroup("LEGS", mapOf("en" to "Legs"))
        val chest = com.github.sebastianp265.aitrainer.integration.exercise.MuscleGroup("CHEST", mapOf("en" to "Chest"))
        val squat = exercises.first().copy(key = "SQUAT", primaryMuscles = listOf(legs), secondaryMuscles = listOf(chest))
        val bench = exercises.first().copy(key = "BENCH", primaryMuscles = listOf(chest), secondaryMuscles = listOf(legs))
        val items = sortedExerciseListItems(listOf(squat, bench), { it.key }, { it.key }, polish)

        assertEquals(listOf("SQUAT"), items.inPrimaryGroup("LEGS").map { it.exercise.key })
        assertEquals(2, items.inPrimaryGroup(null).size)
    }
}
