package de.montagezeit.app.ui.common

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import java.time.LocalTime
import org.junit.Assert.assertEquals

class TimePickerDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dialog_returns_initial_time_when_ok_clicked() {
        var selectedTime: LocalTime? = null
        val initialTime = LocalTime.of(14, 30)

        composeTestRule.setContent {
            TimePickerDialog(
                initialTime = initialTime,
                onTimeSelected = { selectedTime = it },
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("OK").performClick()
        
        assertEquals(initialTime, selectedTime)
    }

    @Test
    fun dialog_can_switch_to_keyboard_input() {
        composeTestRule.setContent {
            TimePickerDialog(
                initialTime = LocalTime.of(12, 0),
                onTimeSelected = {},
                onDismiss = {}
            )
        }

        // Initially showing clock, so button says "Zu Tastatur wechseln"
        composeTestRule.onNodeWithContentDescription("Zu Tastatur wechseln").assertExists()
        composeTestRule.onNodeWithContentDescription("Zu Tastatur wechseln").performClick()
        
        // Now should show "Zu Uhr wechseln"
        composeTestRule.onNodeWithContentDescription("Zu Uhr wechseln").assertExists()
    }
}
