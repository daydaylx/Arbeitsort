package de.montagezeit.app.handler

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import de.montagezeit.app.MainActivity
import de.montagezeit.app.notification.ReminderActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.LocalDate

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [30])
class CheckInActionServiceTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun editAction_startsMainActivity() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val date = LocalDate.of(2024, 1, 5)
        val intent = Intent(application, CheckInActionService::class.java).apply {
            action = ReminderActions.ACTION_EDIT_ENTRY
            putExtra(ReminderActions.EXTRA_DATE, date.toString())
            putExtra(ReminderActions.EXTRA_ACTION_TYPE, ReminderActions.ACTION_EDIT_ENTRY)
        }

        Robolectric.buildService(CheckInActionService::class.java, intent)
            .create()
            .startCommand(0, 0)

        val nextIntent = Shadows.shadowOf(application).nextStartedActivity
        assertNotNull(nextIntent)
        assertEquals(MainActivity::class.java.name, nextIntent?.component?.className)
        assertEquals(date.toString(), nextIntent?.getStringExtra(ReminderActions.EXTRA_DATE))
    }
}
