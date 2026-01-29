package de.montagezeit.app

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodes
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.montagezeit.app.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExportPreviewFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun exportPreviewFlowDoesNotCrash() {
        val editTodayDescription = composeTestRule.activity.getString(R.string.cd_edit_today)

        composeTestRule.onNodeWithText("Verlauf").performClick()
        composeTestRule.onNodeWithContentDescription(editTodayDescription).performClick()

        waitUntilAtLeastOneExists(hasText("Erstellen"))
        composeTestRule.onNodeWithText("Erstellen").performClick()

        waitUntilAtLeastOneExists(hasText("Schließen"))
        composeTestRule.onNodeWithText("Schließen").performClick()

        composeTestRule.onNodeWithText("Einstellungen").performClick()
        composeTestRule.onNodeWithContentDescription("PDF Settings").performClick()

        waitUntilAtLeastOneExists(hasText("Mitarbeiter-Name *"))
        composeTestRule.onNode(hasText("Mitarbeiter-Name *") and hasSetTextAction())
            .performTextInput("Test User")
        composeTestRule.onNodeWithText("Speichern").performClick()

        composeTestRule.onNodeWithText("Benutzerdef.").performClick()
        waitUntilAtLeastOneExists(hasText("Benutzerdefinierter Zeitraum"))
        composeTestRule.onNodeWithText("Exportieren").performClick()

        waitUntilAtLeastOneExists(hasText("Export erfolgreich!"))
        composeTestRule.onNodeWithText("Export erfolgreich!").assertExists()
        composeTestRule.onNodeWithText("Schließen").performClick()
    }

    private fun waitUntilAtLeastOneExists(
        matcher: SemanticsMatcher,
        timeoutMillis: Long = 10_000
    ) {
        composeTestRule.waitUntil(timeoutMillis) {
            composeTestRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
