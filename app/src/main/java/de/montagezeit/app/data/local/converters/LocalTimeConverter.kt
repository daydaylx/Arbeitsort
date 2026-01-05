package de.montagezeit.app.data.local.converters

import androidx.room.TypeConverter
import java.time.LocalTime

class LocalTimeConverter {
    
    @TypeConverter
    fun fromLocalTime(time: LocalTime?): String? {
        return time?.toString()
    }
    
    @TypeConverter
    fun toLocalTime(timeString: String?): LocalTime? {
        return timeString?.let { LocalTime.parse(it) }
    }
}
