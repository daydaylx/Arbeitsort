package de.montagezeit.app

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodes
import androidx.compose.ui.test.onAllNodesWithText
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
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ExportPreviewFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun exportPreviewFlowDoesNotCrash() {
        val editTodayDescription = composeTestRule.activity.getString(R.string.cd_edit_today)
        val today = LocalDate.now().toString()

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
        composeTestRule.onNodeWithText("Vorschau").performClick()

        waitUntilAtLeastOneExists(hasText("PDF erstellen"))
        composeTestRule.onNodeWithContentDescription("ExportPreviewRow-$today").performClick()

        waitUntilAtLeastOneExists(hasText("Speichern"))
        composeTestRule.onNodeWithText("Speichern").performClick()

        waitUntilAtLeastOneExists(hasText("Gespeichert!"))
        composeTestRule.onAllNodesWithText("Schließen").onLast().performClick()

        waitUntilAtLeastOneExists(hasText("PDF erstellen"))
        composeTestRule.onNodeWithText("PDF erstellen").performClick()

        waitUntilAtLeastOneExists(hasText("PDF erstellt"))
        composeTestRule.onNodeWithText("PDF erstellt").assertExists()
        composeTestRule.onNodeWithText("Zurück zur Vorschau").performClick()
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
