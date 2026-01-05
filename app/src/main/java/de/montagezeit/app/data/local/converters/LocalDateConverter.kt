package de.montagezeit.app.data.local.converters

import androidx.room.TypeConverter
import java.time.LocalDate

class LocalDateConverter {
    
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? {
        return date?.toString()
    }
    
    @TypeConverter
    fun toLocalDate(dateString: String?): LocalDate? {
        return dateString?.let { LocalDate.parse(it) }
    }
}
