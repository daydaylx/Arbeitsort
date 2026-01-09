package de.montagezeit.app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateTravelCompensationTest {

    private val useCase = CalculateTravelCompensation()

    @Test
    fun `calculates paid minutes for 100 km`() {
        val result = useCase(
            fromLabel = "A",
            toLabel = "B",
            distanceKm = 100.0,
            roundingStepMinutes = 0
        )
        assertEquals(60, result.paidMinutes)
    }

    @Test
    fun `calculates paid minutes for 150 km`() {
        val result = useCase(
            fromLabel = "A",
            toLabel = "B",
            distanceKm = 150.0,
            roundingStepMinutes = 0
        )
        assertEquals(90, result.paidMinutes)
    }

    @Test
    fun `rounds paid minutes to step`() {
        val result = useCase(
            fromLabel = "A",
            toLabel = "B",
            distanceKm = 110.0,
            roundingStepMinutes = 15
        )
        assertEquals(75, result.paidMinutes)
    }
}
