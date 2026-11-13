package com.github.sebastianp265.aitrainer.feature.progress.impl

import com.github.sebastianp265.aitrainer.integration.exercise.MuscleGroup
import com.github.sebastianp265.aitrainer.integration.volume.TargetSource
import com.github.sebastianp265.aitrainer.integration.volume.WeeklySets
import com.github.sebastianp265.aitrainer.integration.volume.WeeklyTarget
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class VolumeRowsUTest {

    private val groups = listOf("CHEST", "BACK", "ABS").map { MuscleGroup(it, mapOf("en" to it)) }
    private val targets = listOf(
        WeeklyTarget("CHEST", 10, 16, TargetSource.DEFAULT, null, null),
        WeeklyTarget("BACK", 12, 18, TargetSource.DEFAULT, null, null),
        WeeklyTarget("ABS", 6, 10, TargetSource.DEFAULT, null, null),
    )

    @Test
    fun `the weekly average divides all sets of the range by its number of weeks`() {
        val weeks = listOf(
            WeeklySets(LocalDate(2026, 10, 19), mapOf("CHEST" to 10.0, "ABS" to 2.0)),
            WeeklySets(LocalDate(2026, 10, 26), mapOf("CHEST" to 14.0)),
        )

        val average = averageSetsPerWeek(weeks, rangeDays = 28)

        assertEquals(6.0, average.getValue("CHEST"))
        assertEquals(0.5, average.getValue("ABS"))
    }

    @Test
    fun `rows are ordered by the most sets and groups without sets have zero`() {
        val rows = volumeRows(groups, targets, done = mapOf("BACK" to 15.5, "CHEST" to 13.0))

        assertEquals(listOf("BACK", "CHEST", "ABS"), rows.map { it.group.key })
        assertEquals(0.0, rows.last().done)
    }

    @Test
    fun `a row is below, in or above its target`() {
        val rows = volumeRows(groups, targets, done = mapOf("CHEST" to 9.5, "BACK" to 15.0, "ABS" to 11.0))

        assertEquals(TargetStatus.BELOW, rows.single { it.group.key == "CHEST" }.status())
        assertEquals(TargetStatus.IN_TARGET, rows.single { it.group.key == "BACK" }.status())
        assertEquals(TargetStatus.ABOVE, rows.single { it.group.key == "ABS" }.status())
    }

    @Test
    fun `the edges of the range are in target`() {
        val rows = volumeRows(groups, targets, done = mapOf("CHEST" to 10.0, "ABS" to 10.0))

        assertEquals(TargetStatus.IN_TARGET, rows.single { it.group.key == "CHEST" }.status())
        assertEquals(TargetStatus.IN_TARGET, rows.single { it.group.key == "ABS" }.status())
    }
}
