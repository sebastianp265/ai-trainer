package com.github.sebastianp265.aitrainer.domain.stats

import com.github.sebastianp265.aitrainer.domain.model.LoggedSet
import com.github.sebastianp265.aitrainer.domain.model.MuscleGroup
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class TrainingVolumeUTest {

    private val date = LocalDate(2026, 10, 20)

    private fun benchPress(isWarmup: Boolean = false) =
        LoggedSet(date, listOf(MuscleGroup.CHEST), listOf(MuscleGroup.TRICEPS, MuscleGroup.FRONT_DELTS), isWarmup)

    private fun dip() =
        LoggedSet(date, listOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS), listOf(MuscleGroup.FRONT_DELTS), isWarmup = false)

    @Test
    fun `primary muscles count a full set, secondary half, warm-ups nothing`() {
        val sets = listOf(benchPress(isWarmup = true), benchPress(), benchPress(), benchPress(), dip(), dip())

        assertEquals(
            listOf(
                MuscleGroupVolume(MuscleGroup.CHEST, 5.0),
                MuscleGroupVolume(MuscleGroup.TRICEPS, 3.5),
                MuscleGroupVolume(MuscleGroup.FRONT_DELTS, 2.5),
            ),
            setsPerMuscleGroup(sets),
        )
    }
}
