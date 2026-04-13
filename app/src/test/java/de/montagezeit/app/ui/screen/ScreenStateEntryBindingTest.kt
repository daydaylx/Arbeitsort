package de.montagezeit.app.ui.screen

import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.ui.screen.overview.OverviewMetrics
import de.montagezeit.app.ui.screen.overview.OverviewPeriod
import de.montagezeit.app.ui.screen.overview.OverviewScreenState
import de.montagezeit.app.ui.screen.today.TodayScreenState
import de.montagezeit.app.ui.screen.today.TodayUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ScreenStateEntryBindingTest {

    @Test
    fun `today screen hides stale selected entry while loading a different date`() {
        val selectedDate = LocalDate.of(2026, 3, 21)
        val staleEntry = WorkEntry(date = selectedDate.minusDays(1))

        val state = TodayScreenState(
            uiState = TodayUiState.Loading,
            selectedEntry = staleEntry,
            selectedDate = selectedDate,
            todayDate = selectedDate,
            weekDaysUi = emptyList(),
            loadingActions = emptySet()
        )

        assertNull(state.currentEntry)
        assertEquals(true, state.showInitialLoading)
    }

    @Test
    fun `today screen keeps selected entry when it matches the selected date`() {
        val selectedDate = LocalDate.of(2026, 3, 21)
        val currentEntry = WorkEntry(date = selectedDate)

        val state = TodayScreenState(
            uiState = TodayUiState.Loading,
            selectedEntry = currentEntry,
            selectedDate = selectedDate,
            todayDate = selectedDate,
            weekDaysUi = emptyList(),
            loadingActions = emptySet()
        )

        assertEquals(currentEntry, state.currentEntry)
    }

    @Test
    fun `today screen does not expose success fallback from a different date`() {
        val selectedDate = LocalDate.of(2026, 3, 21)
        val staleSuccessEntry = WorkEntry(date = selectedDate.minusDays(1))

        val state = TodayScreenState(
            uiState = TodayUiState.Success(staleSuccessEntry),
            selectedEntry = null,
            selectedDate = selectedDate,
            todayDate = selectedDate,
            weekDaysUi = emptyList(),
            loadingActions = emptySet()
        )

        assertNull(state.currentEntry)
    }

    @Test
    fun `overview screen hides stale selected entry for a different date`() {
        val selectedDate = LocalDate.of(2026, 3, 21)
        val staleEntry = WorkEntry(date = selectedDate.minusDays(1))

        val state = OverviewScreenState(
            selectedDate = selectedDate,
            selectedPeriod = OverviewPeriod.DAY,
            selectedEntry = staleEntry,
            metrics = null,
            isLoading = true,
            errorMessage = null
        )

        assertNull(state.currentEntry)
        assertEquals(true, state.showInitialLoading)
    }

    @Test
    fun `overview screen shows selected entry when date matches`() {
        val selectedDate = LocalDate.of(2026, 3, 21)
        val currentEntry = WorkEntry(date = selectedDate)

        val state = OverviewScreenState(
            selectedDate = selectedDate,
            selectedPeriod = OverviewPeriod.DAY,
            selectedEntry = currentEntry,
            metrics = OverviewMetrics(),
            isLoading = false,
            errorMessage = null
        )

        assertEquals(currentEntry, state.currentEntry)
    }

    @Test
    fun `overview screen exposes travel data only for matching selected date`() {
        val selectedDate = LocalDate.of(2026, 3, 21)
        val staleEntryWithTravel = WorkEntryWithTravelLegs(
            workEntry = WorkEntry(date = selectedDate.minusDays(1)),
            travelLegs = emptyList()
        )

        val state = OverviewScreenState(
            selectedDate = selectedDate,
            selectedPeriod = OverviewPeriod.DAY,
            selectedEntry = staleEntryWithTravel.workEntry,
            selectedEntryWithTravel = staleEntryWithTravel,
            metrics = OverviewMetrics(),
            isLoading = false,
            errorMessage = null
        )

        assertNull(state.currentEntryWithTravel)
        assertEquals(0, state.currentTravelLegs.size)
    }
}
