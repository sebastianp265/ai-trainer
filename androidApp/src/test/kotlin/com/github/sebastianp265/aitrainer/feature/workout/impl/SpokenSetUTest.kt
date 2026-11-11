package com.github.sebastianp265.aitrainer.feature.workout.impl

import kotlin.test.Test
import kotlin.test.assertEquals

class SpokenSetUTest {

    @Test
    fun `weight comes before reps when no unit is said`() {
        assertEquals(SpokenSet(80.0, 8, null), parseSpokenSet("80 na 8"))
    }

    @Test
    fun `a number said with kg is the weight wherever it stands`() {
        assertEquals(SpokenSet(80.0, 8, null), parseSpokenSet("8 powtórzeń 80 kilo"))
    }

    @Test
    fun `decimal commas and an RPE are understood`() {
        assertEquals(SpokenSet(82.5, 6, 8.5), parseSpokenSet("82,5 kg 6 powtórzeń RPE 8,5"))
    }

    @Test
    fun `text without exactly two numbers or with an impossible value is not a set`() {
        assertEquals(null, parseSpokenSet("bardzo ciężko"))
        assertEquals(null, parseSpokenSet("80 na 8 na 3"))
        assertEquals(null, parseSpokenSet("80 na 8 rpe 12"))
        assertEquals(null, parseSpokenSet("80 na 7.5"))
    }
}
