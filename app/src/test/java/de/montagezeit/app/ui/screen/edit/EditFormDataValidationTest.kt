package de.montagezeit.app.ui.screen.edit

import de.montagezeit.app.R
import de.montagezeit.app.data.local.entity.DayType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class EditFormDataValidationTest {

    @Test
    fun `validate should return empty list for valid data`() {
        val formData = validFormData()

        val errors = formData.validate()

        assertTrue(errors.isEmpty())
        assertTrue(formData.isValid())
    }

    @Test
    fun `validate should return MissingDayLocation when day location is blank`() {
        val formData = validFormData(dayLocationLabel = " ")

        val errors = formData.validate()

        assertTrue(errors.any { it is ValidationError.MissingDayLocation })
        assertFalse(formData.isValid())
    }

    @Test
    fun `validate should return WorkEndBeforeStart when workEnd equals workStart`() {
        val formData = validFormData(
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
        val formData = validFormData(
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
        val formData = validFormData(
            breakMinutes = -10
        )

        val errors = formData.validate()

        assertTrue(errors.any { it is ValidationError.NegativeBreakMinutes })
        assertFalse(formData.isValid())
    }

    @Test
    fun `validate should return BreakLongerThanWorkTime when break exceeds work duration`() {
        val formData = validFormData(
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 600
        )

        val errors = formData.validate()

        assertTrue(errors.any { it is ValidationError.BreakLongerThanWorkTime })
        assertFalse(formData.isValid())
    }

    @Test
    fun `validate should allow breakMinutes equal to work duration`() {
        val formData = validFormData(
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 540
        )

        val errors = formData.validate()

        assertFalse(errors.any { it is ValidationError.BreakLongerThanWorkTime })
    }

    @Test
    fun `validate should return TravelArriveBeforeStart when travelArrive equals travelStart`() {
        val formData = validFormData(
            travelStartTime = LocalTime.of(7, 0),
            travelArriveTime = LocalTime.of(7, 0)
        )

        val errors = formData.validate()

        assertTrue(errors.any { it is ValidationError.TravelArriveBeforeStart })
        assertFalse(formData.isValid())
    }

    @Test
    fun `validate should allow overnight travel when travelArrive is before travelStart`() {
        val formData = validFormData(
            travelStartTime = LocalTime.of(23, 0),
            travelArriveTime = LocalTime.of(1, 0)
        )

        val errors = formData.validate()

        assertFalse(errors.any { it is ValidationError.TravelArriveBeforeStart })
        assertTrue(formData.isValid())
    }

    @Test
    fun `validate should not check travel times when only travelStart is set`() {
        val formData = validFormData(
            travelStartTime = LocalTime.of(7, 0),
            travelArriveTime = null
        )

        val errors = formData.validate()

        assertFalse(errors.any { it is ValidationError.TravelArriveBeforeStart })
        assertTrue(formData.isValid())
    }

    @Test
    fun `validate should not check travel times when only travelArrive is set`() {
        val formData = validFormData(
            travelStartTime = null,
            travelArriveTime = LocalTime.of(8, 0)
        )

        val errors = formData.validate()

        assertFalse(errors.any { it is ValidationError.TravelArriveBeforeStart })
        assertTrue(formData.isValid())
    }

    @Test
    fun `validate should return multiple errors when multiple validations fail`() {
        val formData = validFormData(
            breakMinutes = -30,
            travelStartTime = LocalTime.of(7, 30),
            travelArriveTime = LocalTime.of(7, 30)
        )

        val errors = formData.validate()

        assertEquals(2, errors.size)
        assertTrue(errors.any { it is ValidationError.NegativeBreakMinutes })
        assertTrue(errors.any { it is ValidationError.TravelArriveBeforeStart })
        assertFalse(formData.isValid())
    }

    @Test
    fun `validate should return multiple errors including work time and break`() {
        val formData = validFormData(
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(12, 0),
            breakMinutes = 300,
            travelStartTime = LocalTime.of(7, 30),
            travelArriveTime = LocalTime.of(7, 30)
        )

        val errors = formData.validate()

        assertEquals(2, errors.size)
        assertTrue(errors.any { it is ValidationError.BreakLongerThanWorkTime })
        assertTrue(errors.any { it is ValidationError.TravelArriveBeforeStart })
        assertFalse(formData.isValid())
    }

    @Test
    fun `validate should accept zero break minutes`() {
        val formData = validFormData(
            breakMinutes = 0
        )

        val errors = formData.validate()

        assertTrue(errors.isEmpty())
        assertTrue(formData.isValid())
    }

    @Test
    fun `validate should work with edge case times`() {
        val formData = validFormData(
            workStart = LocalTime.of(23, 59),
            workEnd = LocalTime.of(23, 59, 59),
            breakMinutes = 0
        )

        val errors = formData.validate()

        assertTrue(errors.isEmpty())
        assertTrue(formData.isValid())
    }

    @Test
    fun `validate should handle midnight crossing correctly for work time`() {
        val formData = validFormData(
            workStart = LocalTime.of(22, 0),
            workEnd = LocalTime.of(2, 0),
            breakMinutes = 60
        )

        val errors = formData.validate()

        assertTrue(errors.any { it is ValidationError.WorkEndBeforeStart })
    }

    @Test
    fun `ValidationError should map to expected string resources`() {
        assertEquals(
            R.string.edit_validation_missing_day_location,
            ValidationError.MissingDayLocation.messageRes
        )
        assertEquals(
            R.string.edit_validation_work_end_before_start,
            ValidationError.WorkEndBeforeStart.messageRes
        )
        assertEquals(
            R.string.edit_validation_negative_break,
            ValidationError.NegativeBreakMinutes.messageRes
        )
        assertEquals(
            R.string.edit_validation_break_longer_than_work,
            ValidationError.BreakLongerThanWorkTime.messageRes
        )
        assertEquals(
            R.string.edit_validation_travel_arrive_before_start,
            ValidationError.TravelArriveBeforeStart.messageRes
        )
    }

    private fun validFormData(
        dayType: DayType = DayType.WORK,
        workStart: LocalTime = LocalTime.of(8, 0),
        workEnd: LocalTime = LocalTime.of(17, 0),
        breakMinutes: Int = 60,
        dayLocationLabel: String = "Baustelle A",
        travelStartTime: LocalTime? = null,
        travelArriveTime: LocalTime? = null
    ): EditFormData {
        return EditFormData(
            dayType = dayType,
            workStart = workStart,
            workEnd = workEnd,
            breakMinutes = breakMinutes,
            dayLocationLabel = dayLocationLabel,
            travelStartTime = travelStartTime,
            travelArriveTime = travelArriveTime
        )
    }
}
