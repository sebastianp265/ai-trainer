package com.github.sebastianp265.aitrainer.feature.coach

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.github.sebastianp265.aitrainer.feature.coach.impl.CoachMemoryTags
import com.github.sebastianp265.aitrainer.feature.coach.impl.CoachTags
import com.github.sebastianp265.aitrainer.feature.settings.impl.SettingsTags
import com.github.sebastianp265.aitrainer.shared.e2e.Screen

class CoachScreen(compose: ComposeTestRule) : Screen(compose) {

    fun assertTopic(text: String) = waitForText(text)

    fun assertKeyPrompt() = waitForText("The coach needs an API key")

    fun openApiKeyFromPrompt() {
        waitFor(hasTestTag(CoachTags.OPEN_API_KEY))
        compose.onNode(hasTestTag(CoachTags.OPEN_API_KEY)).performClick()
    }

    fun assertGreeting() {
        waitForText("Hi! I am your coach", substring = true)
        waitForText("Write me a plan for this week")
    }

    fun tapSuggestion(text: String) = clickText(text)

    fun tapQuickQuestion(text: String) {
        waitForText(text)
        clickText(text)
    }

    fun back() = compose.onNode(hasContentDescription("Back")).performClick()

    fun send(message: String) {
        waitFor(hasTestTag(CoachTags.INPUT))
        compose.onNode(hasTestTag(CoachTags.INPUT)).performTextInput(message)
        compose.onNode(hasTestTag(CoachTags.SEND)).performClick()
    }

    fun assertCoachSaid(text: String) = waitForText(text)

    fun assertYouSaid(text: String) = waitForText(text)

    fun assertChecked(text: String) = waitForText("Checked: $text", substring = true)

    fun assertProposalShown(summary: String) {
        waitFor(hasTestTag(CoachTags.PROPOSAL))
        waitForText(summary)
    }

    fun assertProposalState(text: String) = waitForText(text, substring = true)

    fun undo() {
        waitFor(hasTestTag(CoachTags.UNDO))
        compose.onNode(hasTestTag(CoachTags.UNDO), useUnmergedTree = true).performClick()
    }

    fun accept() {
        waitFor(hasTestTag(CoachTags.ACCEPT))
        compose.onAllNodes(hasTestTag(CoachTags.ACCEPT), useUnmergedTree = true)[0].performClick()
    }

    fun assertReviewShown() {
        waitFor(hasTestTag(CoachTags.REVIEW))
        waitForText("WEEKLY REVIEW", substring = true)
    }

    fun acceptAll() {
        waitForText("Accept all")
        compose.onAllNodes(hasTestTag(CoachTags.ACCEPT), useUnmergedTree = true)[0].performClick()
    }

    fun openPreview() {
        waitFor(hasTestTag(CoachTags.PREVIEW))
        compose.onAllNodes(hasTestTag(CoachTags.PREVIEW), useUnmergedTree = true)[0].performClick()
    }

    fun assertPreviewShows(text: String) = waitForText(text)

    fun assertOfflineBanner() = waitFor(hasTestTag(CoachTags.OFFLINE_BANNER))

    fun assertNotSent() = waitForText("Not sent.")

    fun retry() {
        waitFor(hasTestTag(CoachTags.RETRY))
        compose.onNode(hasTestTag(CoachTags.RETRY)).performClick()
    }

    fun assertErrorCard(title: String) {
        waitFor(hasTestTag(CoachTags.ERROR_CARD))
        waitForText(title)
    }

    fun tapErrorAction() = compose.onNode(hasTestTag(CoachTags.ERROR_ACTION)).performClick()

    fun assertPendingProposalBanner() = waitForText("THE COACH HAS A PROPOSAL")

    fun openMemory() {
        compose.onAllNodes(hasText("Settings") and hasClickAction()).onFirst().performClick()
        waitFor(hasTestTag(SettingsTags.MEMORY_ROW))
        compose.onNode(hasTestTag(SettingsTags.MEMORY_ROW)).performClick()
        waitForText("Coach notes")
    }

    fun assertNote(text: String) = waitForText(text)

    fun deleteFirstNote() {
        compose.onAllNodes(hasTestTag(CoachMemoryTags.DELETE_NOTE))[0].performClick()
        compose.onNode(hasTestTag(CoachMemoryTags.CONFIRM_DELETE)).performClick()
    }

    fun assertNoNotes() = waitForText("The coach has not written any notes yet.")

    fun editProfile(field: String, value: String) {
        compose.onNode(hasTestTag(CoachMemoryTags.profile(field))).performClick()
        val input = compose.onNode(hasTestTag(CoachMemoryTags.EDIT_FIELD))
        input.performTextClearance()
        input.performTextInput(value)
        compose.onNode(hasTestTag(CoachMemoryTags.SAVE)).performClick()
    }

    fun assertProfile(value: String) = waitForText(value)
}
