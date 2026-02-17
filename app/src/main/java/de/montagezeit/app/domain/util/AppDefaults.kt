package de.montagezeit.app.domain.util

import java.time.LocalTime

object AppDefaults {
    val WORK_START: LocalTime = LocalTime.of(8, 0)
    val WORK_END: LocalTime = LocalTime.of(19, 0)
    const val BREAK_MINUTES = 60
    const val DEFAULT_CITY = "Leipzig"
    const val LOCATION_RADIUS_KM = 30
}
