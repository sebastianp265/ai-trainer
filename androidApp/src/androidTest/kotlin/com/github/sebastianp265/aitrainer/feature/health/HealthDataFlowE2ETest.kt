package com.github.sebastianp265.aitrainer.feature.health

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.integration.health.HealthDataType
import com.github.sebastianp265.aitrainer.shared.e2e.AbstractE2ETest
import com.github.sebastianp265.aitrainer.shared.e2e.FakeHealthService
import org.junit.runner.RunWith
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class HealthDataFlowE2ETest : AbstractE2ETest() {

    private val healthData = HealthDataScreen(compose)

    @Test
    fun `a data type switched off stays off until it is switched on again`() {
        launchApp(healthDataSource = FakeHealthService())
        healthData.open()
        healthData.assertOn(HealthDataType.STEPS)

        healthData.switchOff(HealthDataType.STEPS)
        healthData.assertOff(HealthDataType.STEPS)

        healthData.switchOn(HealthDataType.STEPS)
        healthData.assertOn(HealthDataType.STEPS)
    }
}
