package com.github.sebastianp265.aitrainer.feature.settings.impl

import com.github.sebastianp265.aitrainer.shared.database.impl.temporaryDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultSettingsServiceUTest {

    private val preferences = temporaryDataStore("settings")
    private val service = DefaultSettingsService(preferences)

    @Test
    fun `targets are shown and the coach is on until the owner switches it off`() = runTest {
        assertEquals(true, service.observeShowTargets().first())
        assertEquals(true, service.observeCoachEnabled().first())
    }

    @Test
    fun `the show targets switch is remembered`() = runTest {
        service.setShowTargets(false)

        assertEquals(false, service.observeShowTargets().first())
    }

    @Test
    fun `the coach switch is remembered`() = runTest {
        service.setCoachEnabled(false)

        assertEquals(false, service.observeCoachEnabled().first())
    }
}
