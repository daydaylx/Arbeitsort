package de.montagezeit.app.data.location

import de.montagezeit.app.domain.model.LocationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationAcquisitionStrategyTest {

    @Test
    fun `normalizeTotalTimeout setzt Mindestwert`() {
        assertEquals(1000L, LocationAcquisitionStrategy.normalizeTotalTimeout(500L))
        assertEquals(2500L, LocationAcquisitionStrategy.normalizeTotalTimeout(2500L))
    }

    @Test
    fun `stageOneTimeout begrenzt sinnvoll fuer kleine mittlere und grosse Timeouts`() {
        assertEquals(1000L, LocationAcquisitionStrategy.stageOneTimeout(1000L))
        assertEquals(2500L, LocationAcquisitionStrategy.stageOneTimeout(4000L))
        assertEquals(7000L, LocationAcquisitionStrategy.stageOneTimeout(20000L))
    }

    @Test
    fun `shouldUseLastKnown nutzt nur hinreichend genaue Success Werte`() {
        assertTrue(
            LocationAcquisitionStrategy.shouldUseLastKnown(
                LocationResult.Success(lat = 1.0, lon = 2.0, accuracyMeters = 1500f)
            )
        )
        assertFalse(
            LocationAcquisitionStrategy.shouldUseLastKnown(
                LocationResult.Success(lat = 1.0, lon = 2.0, accuracyMeters = 1501f)
            )
        )
        assertFalse(LocationAcquisitionStrategy.shouldUseLastKnown(LocationResult.Timeout))
        assertFalse(LocationAcquisitionStrategy.shouldUseLastKnown(null))
    }

    @Test
    fun `shouldTryHighAccuracy entscheidet je nach stage one ergebnis`() {
        assertFalse(
            LocationAcquisitionStrategy.shouldTryHighAccuracy(
                LocationResult.Success(lat = 1.0, lon = 2.0, accuracyMeters = 900f)
            )
        )
        assertTrue(
            LocationAcquisitionStrategy.shouldTryHighAccuracy(
                LocationResult.Success(lat = 1.0, lon = 2.0, accuracyMeters = 1500f)
            )
        )
        assertTrue(LocationAcquisitionStrategy.shouldTryHighAccuracy(LocationResult.LowAccuracy(5000f)))
        assertTrue(LocationAcquisitionStrategy.shouldTryHighAccuracy(LocationResult.Timeout))
        assertTrue(LocationAcquisitionStrategy.shouldTryHighAccuracy(LocationResult.Unavailable))
        assertFalse(LocationAcquisitionStrategy.shouldTryHighAccuracy(LocationResult.SkippedByUser))
    }

    @Test
    fun `hasEnoughTimeForStageTwo prueft untergrenze`() {
        assertFalse(LocationAcquisitionStrategy.hasEnoughTimeForStageTwo(2999L))
        assertTrue(LocationAcquisitionStrategy.hasEnoughTimeForStageTwo(3000L))
    }

    @Test
    fun `chooseBetterResult bevorzugt success dann bessere low accuracy`() {
        val stageTwoSuccess = LocationResult.Success(lat = 1.0, lon = 1.0, accuracyMeters = 500f)
        assertEquals(
            stageTwoSuccess,
            LocationAcquisitionStrategy.chooseBetterResult(LocationResult.Timeout, stageTwoSuccess)
        )

        val stageOneSuccess = LocationResult.Success(lat = 2.0, lon = 2.0, accuracyMeters = 600f)
        assertEquals(
            stageOneSuccess,
            LocationAcquisitionStrategy.chooseBetterResult(stageOneSuccess, LocationResult.Timeout)
        )

        val betterLowAccuracy = LocationResult.LowAccuracy(1800f)
        assertEquals(
            betterLowAccuracy,
            LocationAcquisitionStrategy.chooseBetterResult(
                LocationResult.LowAccuracy(2200f),
                betterLowAccuracy
            )
        )

        assertEquals(
            LocationResult.Unavailable,
            LocationAcquisitionStrategy.chooseBetterResult(
                LocationResult.Unavailable,
                LocationResult.Timeout
            )
        )
    }

    @Test
    fun `toLocationResult klassifiziert nach genauigkeit`() {
        val success = LocationAcquisitionStrategy.toLocationResult(
            lat = 51.3,
            lon = 12.3,
            accuracyMeters = 3000f
        )
        val lowAccuracy = LocationAcquisitionStrategy.toLocationResult(
            lat = 51.3,
            lon = 12.3,
            accuracyMeters = 3001f
        )

        assertTrue(success is LocationResult.Success)
        assertTrue(lowAccuracy is LocationResult.LowAccuracy)
    }
}
