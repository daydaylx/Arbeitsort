package de.montagezeit.app.ui.screen.travel

import de.montagezeit.app.data.local.entity.TravelSource

enum class TravelStatus {
    Idle,
    Loading,
    Success,
    Error
}

data class TravelUiState(
    val fromLabel: String = "",
    val toLabel: String = "",
    val manualDistanceKm: String = "",
    val distanceKm: Double? = null,
    val paidMinutes: Int? = null,
    val paidHoursDisplay: String? = null,
    val source: TravelSource? = null,
    val status: TravelStatus = TravelStatus.Idle,
    val errorMessage: String? = null
)
