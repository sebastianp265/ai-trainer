package com.github.sebastianp265.aitrainer.feature.volume.impl

import com.github.sebastianp265.aitrainer.shared.volume.MuscleSetRow
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class WeeklySetsRulesUTest {

    private val monday = LocalDate(2026, 11, 9)

    private fun row(date: LocalDate, group: String, primary: Boolean) = MuscleSetRow(date, group, primary)

    @Test
    fun `a set counts one for the primary group and half for each secondary group`() {
        val weeks = weeklySets(
            listOf(
                row(monday, "CHEST", true), row(monday, "ARMS", false), row(monday, "SHOULDERS", false),
                row(monday, "CHEST", true), row(monday, "ARMS", false), row(monday, "SHOULDERS", false),
            ),
        )

        assertEquals(mapOf("CHEST" to 2.0, "ARMS" to 1.0, "SHOULDERS" to 1.0), weeks.single().setsByGroup)
    }

    @Test
    fun `the week runs from Monday to Sunday`() {
        val sunday = LocalDate(2026, 11, 8)
        val nextMonday = LocalDate(2026, 11, 16)

        val weeks = weeklySets(listOf(row(sunday, "BACK", true), row(monday, "BACK", true), row(LocalDate(2026, 11, 15), "BACK", true), row(nextMonday, "BACK", true)))

        assertEquals(listOf(LocalDate(2026, 11, 2), monday, nextMonday), weeks.map { it.weekStart })
        assertEquals(listOf(1.0, 2.0, 1.0), weeks.map { it.setsByGroup.getValue("BACK") })
    }

    @Test
    fun `no sets give no weeks`() {
        assertEquals(emptyList(), weeklySets(emptyList()))
    }
}
