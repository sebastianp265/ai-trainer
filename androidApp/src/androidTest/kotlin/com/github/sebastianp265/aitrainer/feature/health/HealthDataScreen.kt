package com.github.sebastianp265.aitrainer.feature.health

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isOff
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.github.sebastianp265.aitrainer.feature.health.impl.HealthConnectTags
import com.github.sebastianp265.aitrainer.integration.health.HealthDataType
import com.github.sebastianp265.aitrainer.shared.e2e.Screen
import com.github.sebastianp265.aitrainer.shared.e2e.TEST_TIMEOUT_MS

class HealthDataScreen(compose: ComposeTestRule) : Screen(compose) {

    fun open() {
        compose.waitUntil(TEST_TIMEOUT_MS) { compose.onAllNodes(hasText("Settings") and hasClickAction()).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(hasText("Settings") and hasClickAction()).onFirst().performClick()
        waitForText("What your coach can access")
    }

    fun switchOff(type: HealthDataType) = switchOf(type).performClick()

    fun switchOn(type: HealthDataType) = switchOf(type).performClick()

    fun assertOn(type: HealthDataType) = waitFor(switchTag(type) and isOn())

    fun assertOff(type: HealthDataType) = waitFor(switchTag(type) and isOff())

    private fun switchOf(type: HealthDataType) = run {
        waitFor(hasScrollAction())
        compose.onNode(hasScrollAction()).performScrollToNode(switchTag(type))
        compose.onNode(switchTag(type))
    }

    private fun switchTag(type: HealthDataType): SemanticsMatcher = hasTestTag(HealthConnectTags.switch(type))
}
