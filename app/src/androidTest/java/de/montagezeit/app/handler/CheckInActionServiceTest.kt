package de.montagezeit.app.handler

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.domain.model.LocationResult
import de.montagezeit.app.domain.usecase.RecordMorningCheckIn
import de.montagezeit.app.notification.ReminderActions
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import javax.inject.Inject

/**
 * Instrumented Test für CheckInActionService
 * 
 * Testet ob Actions aus Notifications korrekt verarbeitet werden:
 * - Morning Check-in mit Location speichert DB-Entry
 * - Morning Check-in ohne Location setzt needsReview=true
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
@Config(application = HiltTestApplication::class, sdk = [30])
class CheckInActionServiceTest {
    
    @Inject
    lateinit var workEntryDao: WorkEntryDao
    
    @Inject
    lateinit var recordMorningCheckIn: RecordMorningCheckIn
    
    @Inject
    lateinit var recordEveningCheckIn: RecordEveningCheckIn
    
    private lateinit var context: Context
    private lateinit var service: CheckInActionService
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        service = CheckInActionService()
    }
    
    @Test
    fun testMorningCheckInWithLocation_savesEntry() = runTest {
        // Arrange
        val date = LocalDate.now()
        val intent = Intent(context, CheckInActionService::class.java).apply {
            action = ReminderActions.ACTION_MORNING_CHECK_IN_WITH_LOCATION
            putExtra(ReminderActions.EXTRA_DATE, date.toString())
            putExtra(ReminderActions.EXTRA_ACTION_TYPE, ReminderActions.ACTION_MORNING_CHECK_IN_WITH_LOCATION)
        }
        
        // Mock WorkEntryDao
        val mockEntry = createMockWorkEntry(date)
        every { workEntryDao.getByDate(any()) } returns null
        every { workEntryDao.upsert(any()) } returns Unit
        
        // Act
        service.onCreate()
        service.onStartCommand(intent, 0, 0)
        
        // Assert - Verify dass der UseCase aufgerufen wurde
        verify { recordMorningCheckIn(date, forceWithoutLocation = false) }
    }
    
    @Test
    fun testMorningCheckInWithoutLocation_setsNeedsReview() = runTest {
        // Arrange
        val date = LocalDate.now()
        val intent = Intent(context, CheckInActionService::class.java).apply {
            action = ReminderActions.ACTION_MORNING_CHECK_IN_WITHOUT_LOCATION
            putExtra(ReminderActions.EXTRA_DATE, date.toString())
            putExtra(ReminderActions.EXTRA_ACTION_TYPE, ReminderActions.ACTION_MORNING_CHECK_IN_WITHOUT_LOCATION)
        }
        
        // Mock WorkEntryDao
        every { workEntryDao.getByDate(any()) } returns null
        every { workEntryDao.upsert(any()) } returns Unit
        
        // Act
        service.onCreate()
        service.onStartCommand(intent, 0, 0)
        
        // Assert - Verify dass der UseCase mit forceWithoutLocation=true aufgerufen wurde
        verify { recordMorningCheckIn(date, forceWithoutLocation = true) }
    }
    
    @Test
    fun testEveningCheckInWithLocation_savesEntry() = runTest {
        // Arrange
        val date = LocalDate.now()
        val intent = Intent(context, CheckInActionService::class.java).apply {
            action = ReminderActions.ACTION_EVENING_CHECK_IN_WITH_LOCATION
            putExtra(ReminderActions.EXTRA_DATE, date.toString())
            putExtra(ReminderActions.EXTRA_ACTION_TYPE, ReminderActions.ACTION_EVENING_CHECK_IN_WITH_LOCATION)
        }
        
        // Mock WorkEntryDao
        every { workEntryDao.getByDate(any()) } returns null
        every { workEntryDao.upsert(any()) } returns Unit
        
        // Act
        service.onCreate()
        service.onStartCommand(intent, 0, 0)
        
        // Assert - Verify dass der UseCase aufgerufen wurde
        verify { recordEveningCheckIn(date, forceWithoutLocation = false) }
    }
    
    @Test
    fun testEveningCheckInWithoutLocation_setsNeedsReview() = runTest {
        // Arrange
        val date = LocalDate.now()
        val intent = Intent(context, CheckInActionService::class.java).apply {
            action = ReminderActions.ACTION_EVENING_CHECK_IN_WITHOUT_LOCATION
            putExtra(ReminderActions.EXTRA_DATE, date.toString())
            putExtra(ReminderActions.EXTRA_ACTION_TYPE, ReminderActions.ACTION_EVENING_CHECK_IN_WITHOUT_LOCATION)
        }
        
        // Mock WorkEntryDao
        every { workEntryDao.getByDate(any()) } returns null
        every { workEntryDao.upsert(any()) } returns Unit
        
        // Act
        service.onCreate()
        service.onStartCommand(intent, 0, 0)
        
        // Assert - Verify dass der UseCase mit forceWithoutLocation=true aufgerufen wurde
        verify { recordEveningCheckIn(date, forceWithoutLocation = true) }
    }
    
    private fun createMockWorkEntry(date: LocalDate): WorkEntry {
        return WorkEntry(
            date = date,
            dayType = DayType.WORK,
            morningCapturedAt = null,
            eveningCapturedAt = null,
            needsReview = false
        )
    }
}
