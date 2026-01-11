package de.montagezeit.app.ui.screen.edit

import de.montagezeit.app.data.local.entity.DayType
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalTime

class EditFormDataValidationTest {

    @Test
    fun `validate should return empty list for valid data`() {
        val formData = EditFormData(
            dayType = DayType.WORK,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 60
        )

        val errors = formData.validate()

        assertTrue(errors.isEmpty())
        assertTrue(formData.isValid())
    }

    @Test
    fun `validate should return WorkEndBeforeStart when workEnd equals workStart`() {
        val formData = EditFormData(
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(8, 0),
            breakMinutes = 0
        )

        val errors = formData.validate()

        assertEquals(1, errors.size)
        assertTrue(errors[0] is ValidationError.WorkEndBeforeStart)
        assertFalse(formData.isValid())
    }

    @Test
    fun `validate should return WorkEndBeforeStart when workEnd is before workStart`() {
        val formData = EditFormData(
            workStart = LocalTime.of(17, 0),
            workEnd = LocalTime.of(8, 0),
            breakMinutes = 0
        )

        val errors = formData.validate()

        assertEquals(1, errors.size)
        assertTrue(errors[0] is ValidationError.WorkEndBeforeStart)
    }

    @Test
    fun `validate should return NegativeBreakMinutes when breakMinutes is negative`() {
        val formData = EditFormData(
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = -10
        )

        val errors = formData.validate()

        assertTrue(errors.any { it is ValidationError.NegativeBreakMinutes })
        assertFalse(formData.isValid())
    }

    @Test
    fun `validate should return BreakLongerThanWorkTime when break exceeds work duration`() {
        val formData = EditFormData(
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),  // 9 hours = 540 minutes
            breakMinutes = 600  // 10 hours
        )

        val errors = formData.validate()

        assertTrue(errors.any { it is ValidationError.BreakLongerThanWorkTime })
        assertFalse(formData.isValid())
    }

    @Test
    fun `validate should allow breakMinutes equal to work duration`() {
        val formData = EditFormData(
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),  // 9 hours = 540 minutes
            breakMinutes = 540
        )

        val errors = formData.validate()

        assertFalse(errors.any { it is ValidationError.BreakLongerThanWorkTime })
    }

    @Test
    fun `validate should return TravelArriveBeforeStart when travelArrive equals travelStart`() {
        val formData = EditFormData(
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 60,
            travelStartTime = LocalTime.of(7, 0),
            travelArriveTime = LocalTime.of(7, 0)
        )

        val errors = formData.validate()

        assertTrue(errors.any { it is ValidationError.TravelArriveBeforeStart })
        assertFalse(formData.isValid())
    }

    @Test
    fun `validate should return TravelArriveBeforeStart when travelArrive is before travelStart`() {
        val formData = EditFormData(
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 60,
            travelStartTime = LocalTime.of(7, 30),
            travelArriveTime = LocalTime.of(7, 0)
        )

        val errors = formData.validate()

        assertTrue(errors.any { it is ValidationError.TravelArriveBeforeStart })
    }

    @Test
    fun `validate should not check travel times when only travelStart is set`() {
        val formData = EditFormData(
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 60,
            travelStartTime = LocalTime.of(7, 0),
            travelArriveTime = null
        )

        val errors = formData.validate()

        assertFalse(errors.any { it is ValidationError.TravelArriveBeforeStart })
        assertTrue(formData.isValid())
    }

    @Test
    fun `validate should not check travel times when only travelArrive is set`() {
        val formData = EditFormData(
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 60,
            travelStartTime = null,
            travelArriveTime = LocalTime.of(8, 0)
        )

        val errors = formData.validate()

        assertFalse(errors.any { it is ValidationError.TravelArriveBeforeStart })
        assertTrue(formData.isValid())
    }

    @Test
    fun `validate should return multiple errors when multiple validations fail`() {
        val formData = EditFormData(
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = -30,  // Invalid: negative
            travelStartTime = LocalTime.of(7, 30),
            travelArriveTime = LocalTime.of(7, 0)  // Invalid: arrive before start
        )

        val errors = formData.validate()

        assertEquals(2, errors.size)
        assertTrue(errors.any { it is ValidationError.NegativeBreakMinutes })
        assertTrue(errors.any { it is ValidationError.TravelArriveBeforeStart })
        assertFalse(formData.isValid())
    }

    @Test
    fun `validate should return multiple errors including work time and break`() {
        val formData = EditFormData(
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(12, 0),  // 4 hours = 240 minutes
            breakMinutes = 300,  // Invalid: 5 hours > 4 hours work time
            travelStartTime = LocalTime.of(7, 30),
            travelArriveTime = LocalTime.of(7, 0)  // Invalid: arrive before start
        )

        val errors = formData.validate()

        assertEquals(2, errors.size)
        assertTrue(errors.any { it is ValidationError.BreakLongerThanWorkTime })
        assertTrue(errors.any { it is ValidationError.TravelArriveBeforeStart })
        assertFalse(formData.isValid())
    }

    @Test
    fun `validate should accept zero break minutes`() {
        val formData = EditFormData(
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 0
        )

        val errors = formData.validate()

        assertTrue(errors.isEmpty())
        assertTrue(formData.isValid())
    }

    @Test
    fun `validate should work with edge case times`() {
        val formData = EditFormData(
            workStart = LocalTime.of(23, 59),
            workEnd = LocalTime.of(23, 59, 59),
            breakMinutes = 0
        )

        val errors = formData.validate()

        // workEnd is technically after workStart by 59 seconds
        assertTrue(errors.isEmpty())
        assertTrue(formData.isValid())
    }

    @Test
    fun `validate should handle midnight crossing correctly`() {
        val formData = EditFormData(
            workStart = LocalTime.of(22, 0),
            workEnd = LocalTime.of(2, 0),  // Next day (not supported in current model)
            breakMinutes = 60
        )

        val errors = formData.validate()

        // Current implementation treats this as workEnd before workStart
        assertTrue(errors.any { it is ValidationError.WorkEndBeforeStart })
    }

    @Test
    fun `ValidationError messages should be in German`() {
        assertEquals(
            "Arbeitsende muss nach Arbeitsbeginn liegen",
            ValidationError.WorkEndBeforeStart.message
        )
        assertEquals(
            "Pause kann nicht negativ sein",
            ValidationError.NegativeBreakMinutes.message
        )
        assertEquals(
            "Pause kann nicht länger als die Arbeitszeit sein",
            ValidationError.BreakLongerThanWorkTime.message
        )
        assertEquals(
            "Ankunftszeit muss nach Abfahrtszeit liegen",
            ValidationError.TravelArriveBeforeStart.message
        )
    }
}
