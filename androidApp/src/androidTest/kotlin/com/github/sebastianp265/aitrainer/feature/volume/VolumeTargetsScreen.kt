package com.github.sebastianp265.aitrainer.feature.volume

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import com.github.sebastianp265.aitrainer.feature.volume.impl.VolumeTags
import com.github.sebastianp265.aitrainer.shared.e2e.Screen

class VolumeTargetsScreen(compose: ComposeTestRule) : Screen(compose) {

    fun assertShown() = waitForText("Weekly set targets")

    fun setRange(groupKey: String, from: String, to: String) {
        waitFor(hasTestTag(VolumeTags.min(groupKey)))
        val min = compose.onNode(hasTestTag(VolumeTags.min(groupKey)))
        min.performScrollTo()
        min.performTextClearance()
        min.performTextInput(from)
        val max = compose.onNode(hasTestTag(VolumeTags.max(groupKey)))
        max.performTextClearance()
        max.performTextInput(to)
    }

    fun back() = compose.onNode(hasContentDescription("Back")).performClick()

    fun assertSource(text: String) = waitForText(text, substring = true)

    fun restoreDefaults() {
        waitFor(hasTestTag(VolumeTags.RESTORE_DEFAULTS))
        val button = compose.onNode(hasTestTag(VolumeTags.RESTORE_DEFAULTS))
        button.performScrollTo()
        button.performClick()
    }

    fun switchShowTargets() {
        waitFor(hasTestTag(VolumeTags.SHOW_TARGETS))
        compose.onNode(hasTestTag(VolumeTags.SHOW_TARGETS)).performClick()
    }
}
