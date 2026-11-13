package com.github.sebastianp265.aitrainer.shared.ai.impl

import android.Manifest
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class SdkClaudeClientITest {

    @Test
    fun `the client may reach the Claude API over the internet`() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        assertEquals(PackageManager.PERMISSION_GRANTED, context.checkSelfPermission(Manifest.permission.INTERNET))
    }
}
