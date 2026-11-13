package com.github.sebastianp265.aitrainer.shared.workout

import com.github.sebastianp265.aitrainer.integration.workout.LastTimeGroup
import java.text.NumberFormat
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class LastTimeTextUTest {

    private val numberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("pl")).apply { maximumFractionDigits = 2 }
    private val labels = LastTimeLabels("kg", "RPE", "masa ciała") { numberFormat.format(it) }

    @Test
    fun `several equal sets show sets, reps, weight and RPE`() {
        assertEquals("3×8×80 kg RPE 7,5", LastTimeGroup(3, 8, 80.0, 7.5, 7.5).text(labels))
    }

    @Test
    fun `a single set leaves out the set count`() {
        assertEquals("8×26 kg RPE 9", LastTimeGroup(1, 8, 26.0, 9.0, 9.0).text(labels))
    }

    @Test
    fun `different RPE shows a range`() {
        assertEquals("2×6×50 kg RPE 8–9", LastTimeGroup(2, 6, 50.0, 8.0, 9.0).text(labels))
    }

    @Test
    fun `no RPE leaves the RPE part out`() {
        assertEquals("2×12×15 kg", LastTimeGroup(2, 12, 15.0, null, null).text(labels))
    }

    @Test
    fun `bodyweight sets show the bodyweight label`() {
        assertEquals("3×10 · masa ciała", LastTimeGroup(3, 10, 0.0, null, null).text(labels))
    }

    @Test
    fun `hidden sets are those of the groups that do not fit`() {
        val groups = listOf(
            LastTimeGroup(1, 15, 10.0, null, null),
            LastTimeGroup(1, 12, 12.5, null, null),
            LastTimeGroup(2, 10, 17.5, null, null),
            LastTimeGroup(1, 8, 20.0, null, null),
        )

        assertEquals(3, hiddenSetCount(groups, shownGroupCount = 2))
        assertEquals(0, hiddenSetCount(groups, shownGroupCount = 4))
    }
}
