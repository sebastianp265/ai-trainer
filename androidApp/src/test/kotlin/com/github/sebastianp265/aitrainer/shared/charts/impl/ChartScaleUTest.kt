package com.github.sebastianp265.aitrainer.shared.charts.impl

import kotlin.test.Test
import kotlin.test.assertEquals

class ChartScaleUTest {

    @Test
    fun `body weight gets half kilogram steps around the values`() {
        assertEquals(ChartScale(min = 81.5, max = 83.0, step = 0.5), chartScale(81.6, 82.9))
    }

    @Test
    fun `steps grow with the spread of the values`() {
        assertEquals(ChartScale(min = 6000.0, max = 12000.0, step = 2000.0), chartScale(6200.0, 11800.0))
    }

    @Test
    fun `negative values are covered too`() {
        assertEquals(ChartScale(min = -400.0, max = 200.0, step = 200.0), chartScale(-320.0, 150.0))
    }

    @Test
    fun `equal values get one step of room`() {
        assertEquals(ChartScale(min = 80.0, max = 80.5, step = 0.5), chartScale(80.0, 80.0))
    }
}
