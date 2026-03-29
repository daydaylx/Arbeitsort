package de.montagezeit.app.ui.screen.today

import de.montagezeit.app.data.local.entity.WorkEntry
import javax.inject.Inject
import java.time.LocalDate

class TodayWeekOverviewUseCase @Inject constructor() {
    operator fun invoke(
        selectedDate: LocalDate,
        todayDate: LocalDate,
        entries: List<WorkEntry>
    ): List<WeekDayUi> = buildWeekDayUi(
        selectedDate = selectedDate,
        todayDate = todayDate,
        entries = entries
    )
}
