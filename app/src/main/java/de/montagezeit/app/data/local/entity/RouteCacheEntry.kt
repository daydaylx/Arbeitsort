package de.montagezeit.app.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "route_cache",
    primaryKeys = ["fromLabel", "toLabel"]
)
data class RouteCacheEntry(
    val fromLabel: String,
    val toLabel: String,
    val distanceKm: Double,
    val updatedAt: Long
)
