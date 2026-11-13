package com.github.sebastianp265.aitrainer.feature.settings.impl

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultSettingsServiceUTest {

    private val preferences = PreferenceDataStoreFactory.create(
        produceFile = { Files.createTempDirectory("settings").resolve("settings.preferences_pb").toFile() },
    )
    private val service = DefaultSettingsService(preferences)

    @Test
    fun `targets are shown and the coach is on until the owner switches it off`() = runBlocking {
        assertEquals(true, service.observeShowTargets().first())
        assertEquals(true, service.observeCoachEnabled().first())
    }

    @Test
    fun `the show targets switch is remembered`() = runBlocking {
        service.setShowTargets(false)

        assertEquals(false, service.observeShowTargets().first())
    }

    @Test
    fun `the coach switch is remembered`() = runBlocking {
        service.setCoachEnabled(false)

        assertEquals(false, service.observeCoachEnabled().first())
    }
}
