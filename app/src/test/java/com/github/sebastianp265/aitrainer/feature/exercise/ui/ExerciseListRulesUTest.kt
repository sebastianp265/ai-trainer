package com.github.sebastianp265.aitrainer.feature.exercise.ui

import com.github.sebastianp265.aitrainer.feature.exercise.domain.model.Equipment
import com.github.sebastianp265.aitrainer.feature.exercise.domain.model.Exercise
import com.github.sebastianp265.aitrainer.feature.exercise.domain.model.ExerciseKey
import com.github.sebastianp265.aitrainer.feature.exercise.domain.model.MuscleGroup
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class ExerciseListRulesUTest {

    private val polish = Locale.forLanguageTag("pl")

    private val polishNames = mapOf(
        ExerciseKey.BARBELL_BENCH_PRESS to "Wyciskanie sztangi na ławce płaskiej",
        ExerciseKey.DUMBBELL_BENCH_PRESS to "Wyciskanie hantli na ławce płaskiej",
        ExerciseKey.BARBELL_SHRUG to "Szrugsy ze sztangą",
        ExerciseKey.LAT_PULLDOWN to "Ściąganie drążka wyciągu górnego",
        ExerciseKey.PLANK to "Deska (plank)",
        ExerciseKey.PEC_DECK to "Rozpiętki na maszynie (butterfly)",
        ExerciseKey.CABLE_FLY to "Rozpiętki na wyciągu",
        ExerciseKey.REAR_DELT_FLY to "Odwrotne rozpiętki z hantlami",
    )

    private val englishNames = mapOf(
        ExerciseKey.BARBELL_BENCH_PRESS to "Barbell Bench Press",
        ExerciseKey.DUMBBELL_BENCH_PRESS to "Dumbbell Bench Press",
        ExerciseKey.BARBELL_SHRUG to "Barbell Shrug",
        ExerciseKey.LAT_PULLDOWN to "Lat Pulldown",
        ExerciseKey.PLANK to "Plank",
        ExerciseKey.PEC_DECK to "Pec Deck",
        ExerciseKey.CABLE_FLY to "Cable Fly",
        ExerciseKey.REAR_DELT_FLY to "Rear Delt Fly",
    )

    private fun exercise(id: Long, key: ExerciseKey) =
        Exercise(id, key, listOf(MuscleGroup.CHEST), emptyList(), Equipment.BARBELL)

    private val exercises = polishNames.keys.mapIndexed { i, key -> exercise(i.toLong(), key) }

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
}
