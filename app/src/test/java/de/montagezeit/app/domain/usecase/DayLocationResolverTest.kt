package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.entity.WorkEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DayLocationResolverTest {

    private val date = LocalDate.of(2024, 6, 10)

    @Test
    fun `resolve returns empty string when entry is null`() {
        assertEquals("", DayLocationResolver.resolve(null))
    }

    @Test
    fun `resolve returns empty string when entry dayLocationLabel is blank`() {
        val entry = WorkEntry(date = date, dayLocationLabel = "")
        assertEquals("", DayLocationResolver.resolve(entry))
    }

    @Test
    fun `resolve returns empty string when entry dayLocationLabel is whitespace only`() {
        val entry = WorkEntry(date = date, dayLocationLabel = "   ")
        assertEquals("", DayLocationResolver.resolve(entry))
    }

    @Test
    fun `resolve returns dayLocationLabel when entry has non-blank label`() {
        val entry = WorkEntry(date = date, dayLocationLabel = "Berlin")
        assertEquals("Berlin", DayLocationResolver.resolve(entry))
    }
}
