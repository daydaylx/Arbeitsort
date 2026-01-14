package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.LocationStatus
import de.montagezeit.app.data.local.entity.WorkEntry
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ResolveReviewTest {
    
    private lateinit var workEntryDao: WorkEntryDao
    private lateinit var resolveReview: ResolveReview
    
    private val testDate = LocalDate.of(2024, 1, 15)
    
    @Before
    fun setup() {
        workEntryDao = mockk()
        
        resolveReview = ResolveReview(
            workEntryDao = workEntryDao
        )
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `resolveReview with MORNING scope sets morningLocationLabel and needsReview to false`() = runTest {
        val entry = createTestEntry(needsReview = true)
        coEvery { workEntryDao.getByDate(any()) } returns entry
        coEvery { workEntryDao.upsert(any()) } just Runs
        
        val result = resolveReview(
            date = testDate,
            scope = ReviewScope.MORNING,
            resolvedLabel = "Test City",
            isLeipzig = false
        )
        
        assert(result.needsReview == false) {
            "needsReview sollte false sein"
        }
        assert(result.morningLocationLabel == "Test City") {
            "morningLocationLabel sollte 'Test City' sein"
        }
        assert(result.outsideLeipzigMorning == true) {
            "outsideLeipzigMorning sollte true sein"
        }
        
        coVerify { workEntryDao.upsert(any()) }
    }
    
    @Test
    fun `resolveReview with EVENING scope sets eveningLocationLabel and needsReview to false`() = runTest {
        val entry = createTestEntry(needsReview = true)
        coEvery { workEntryDao.getByDate(any()) } returns entry
        coEvery { workEntryDao.upsert(any()) } just Runs
        
        val result = resolveReview(
            date = testDate,
            scope = ReviewScope.EVENING,
            resolvedLabel = "Test City",
            isLeipzig = false
        )
        
        assert(result.needsReview == false) {
            "needsReview sollte false sein"
        }
        assert(result.eveningLocationLabel == "Test City") {
            "eveningLocationLabel sollte 'Test City' sein"
        }
        assert(result.outsideLeipzigEvening == true) {
            "outsideLeipzigEvening sollte true sein"
        }
        
        coVerify { workEntryDao.upsert(any()) }
    }
    
    @Test
    fun `resolveReview with BOTH scope sets both locationLabels and needsReview to false`() = runTest {
        val entry = createTestEntry(needsReview = true)
        coEvery { workEntryDao.getByDate(any()) } returns entry
        coEvery { workEntryDao.upsert(any()) } just Runs
        
        val result = resolveReview(
            date = testDate,
            scope = ReviewScope.BOTH,
            resolvedLabel = "Test City",
            isLeipzig = false
        )
        
        assert(result.needsReview == false) {
            "needsReview sollte false sein"
        }
        assert(result.morningLocationLabel == "Test City") {
            "morningLocationLabel sollte 'Test City' sein"
        }
        assert(result.eveningLocationLabel == "Test City") {
            "eveningLocationLabel sollte 'Test City' sein"
        }
        
        coVerify { workEntryDao.upsert(any()) }
    }
    
    @Test
    fun `resolveReview with isLeipzig true sets outsideLeipzig to false`() = runTest {
        val entry = createTestEntry(needsReview = true)
        coEvery { workEntryDao.getByDate(any()) } returns entry
        coEvery { workEntryDao.upsert(any()) } just Runs
        
        val result = resolveReview(
            date = testDate,
            scope = ReviewScope.BOTH,
            resolvedLabel = "Leipzig",
            isLeipzig = true
        )
        
        assert(result.outsideLeipzigMorning == false) {
            "outsideLeipzigMorning sollte false sein"
        }
        assert(result.outsideLeipzigEvening == false) {
            "outsideLeipzigEvening sollte false sein"
        }
    }
    
    @Test
    fun `resolveReview with isLeipzig false sets outsideLeipzig to true`() = runTest {
        val entry = createTestEntry(needsReview = true)
        coEvery { workEntryDao.getByDate(any()) } returns entry
        coEvery { workEntryDao.upsert(any()) } just Runs
        
        val result = resolveReview(
            date = testDate,
            scope = ReviewScope.BOTH,
            resolvedLabel = "Berlin",
            isLeipzig = false
        )
        
        assert(result.outsideLeipzigMorning == true) {
            "outsideLeipzigMorning sollte true sein"
        }
        assert(result.outsideLeipzigEvening == true) {
            "outsideLeipzigEvening sollte true sein"
        }
    }
    
    @Test
    fun `resolveReview creates new entry if it does not exist`() = runTest {
        coEvery { workEntryDao.getByDate(any()) } returns null
        coEvery { workEntryDao.upsert(any()) } just Runs
        
        val result = resolveReview(
            date = testDate,
            scope = ReviewScope.BOTH,
            resolvedLabel = "Test City",
            isLeipzig = false
        )
        
        assert(result.date == testDate) {
            "Datum sollte korrekt sein"
        }
        assert(result.needsReview == false) {
            "needsReview sollte false sein"
        }
        
        coVerify { workEntryDao.upsert(any()) }
    }
    
    private fun createTestEntry(needsReview: Boolean = true): WorkEntry {
        return WorkEntry(
            date = testDate,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 60,
            morningCapturedAt = System.currentTimeMillis(),
            eveningCapturedAt = System.currentTimeMillis(),
            morningLocationStatus = if (needsReview) LocationStatus.LOW_ACCURACY else LocationStatus.OK,
            eveningLocationStatus = if (needsReview) LocationStatus.LOW_ACCURACY else LocationStatus.OK,
            needsReview = needsReview
        )
    }
}