package com.github.sebastianp265.aitrainer.feature.workout.impl

import kotlin.test.Test
import kotlin.test.assertEquals

class ElapsedTimeUTest {

    @Test
    fun `elapsed time shows minutes and seconds, and hours when needed`() {
        assertEquals("32:14", formatElapsed(32 * 60 + 14L))
        assertEquals("1:05:09", formatElapsed(3600 + 5 * 60 + 9L))
    }
}
