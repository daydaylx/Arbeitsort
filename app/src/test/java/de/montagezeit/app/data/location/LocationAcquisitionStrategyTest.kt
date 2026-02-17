package de.montagezeit.app.data.location

import de.montagezeit.app.domain.model.LocationResult
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit-Tests für LocationAcquisitionStrategy.
 *
 * LocationAcquisitionStrategy ist ein reines Kotlin-Objekt ohne Android-Abhängigkeiten
 * und kann daher als normaler JVM-Test vollständig abgedeckt werden.
 *
 * Abgedeckte Bereiche:
 * - normalizeTotalTimeout: Minimum-Clamp
 * - stageOneTimeout: Proportional / Capped
 * - shouldUseLastKnown: Genauigkeitsschwelle 1500m
 * - shouldTryHighAccuracy: Alle LocationResult-Typen
 * - hasEnoughTimeForStageTwo: Schwellenwert 3000ms
 * - chooseBetterResult: Priorisierungslogik
 * - toLocationResult: Success vs. LowAccuracy (Schwelle 3000m)
 */
class LocationAcquisitionStrategyTest {

    // -------------------------------------------------------------------------
    // normalizeTotalTimeout
    // -------------------------------------------------------------------------

    @Test
    fun `normalizeTotalTimeout - sehr kleiner Wert bleibt bei Minimum 1000ms`() {
        assertEquals(1000L, LocationAcquisitionStrategy.normalizeTotalTimeout(1L))
    }

    @Test
    fun `normalizeTotalTimeout - 0 ergibt Minimum 1000ms`() {
        assertEquals(1000L, LocationAcquisitionStrategy.normalizeTotalTimeout(0L))
    }

    @Test
    fun `normalizeTotalTimeout - Wert groesser als Minimum bleibt unveraendert`() {
        assertEquals(15000L, LocationAcquisitionStrategy.normalizeTotalTimeout(15000L))
    }

    @Test
    fun `normalizeTotalTimeout - genau Minimum bleibt unveraendert`() {
        assertEquals(1000L, LocationAcquisitionStrategy.normalizeTotalTimeout(1000L))
    }

    // -------------------------------------------------------------------------
    // stageOneTimeout
    // -------------------------------------------------------------------------

    @Test
    fun `stageOneTimeout - sehr grosser Timeout wird auf STAGE_ONE_MAX (7000ms) begrenzt`() {
        // totalTimeout=20000 → min(7000, max(2500, 10000), 20000) = 7000
        assertEquals(7000L, LocationAcquisitionStrategy.stageOneTimeout(20000L))
    }

    @Test
    fun `stageOneTimeout - proportional fuer mittleren Timeout (halbiert)`() {
        // totalTimeout=10000 → min(7000, max(2500, 5000), 10000) = 5000
        assertEquals(5000L, LocationAcquisitionStrategy.stageOneTimeout(10000L))
    }

    @Test
    fun `stageOneTimeout - kleiner Timeout wird auf STAGE_ONE_MIN (2500ms) angehoben`() {
        // totalTimeout=4000 → min(7000, max(2500, 2000), 4000) = 2500
        assertEquals(2500L, LocationAcquisitionStrategy.stageOneTimeout(4000L))
    }

    @Test
    fun `stageOneTimeout - totalTimeout selbst begrenzt wenn kleiner als Min`() {
        // totalTimeout=1000 → min(7000, max(2500, 500), 1000) = 1000
        assertEquals(1000L, LocationAcquisitionStrategy.stageOneTimeout(1000L))
    }

    // -------------------------------------------------------------------------
    // shouldUseLastKnown
    // -------------------------------------------------------------------------

    @Test
    fun `shouldUseLastKnown - gute Genauigkeit (500m) ergibt true`() {
        val result = LocationResult.Success(52.0, 13.0, 500f)
        assertTrue(LocationAcquisitionStrategy.shouldUseLastKnown(result))
    }

    @Test
    fun `shouldUseLastKnown - genau Grenzwert 1500m ergibt true`() {
        val result = LocationResult.Success(52.0, 13.0, 1500f)
        assertTrue(LocationAcquisitionStrategy.shouldUseLastKnown(result))
    }

    @Test
    fun `shouldUseLastKnown - 1501m (knapp ueber Grenze) ergibt false`() {
        val result = LocationResult.Success(52.0, 13.0, 1501f)
        assertFalse(LocationAcquisitionStrategy.shouldUseLastKnown(result))
    }

    @Test
    fun `shouldUseLastKnown - schlechte Genauigkeit (5000m) ergibt false`() {
        val result = LocationResult.Success(52.0, 13.0, 5000f)
        assertFalse(LocationAcquisitionStrategy.shouldUseLastKnown(result))
    }

    @Test
    fun `shouldUseLastKnown - LowAccuracy ergibt false`() {
        assertFalse(LocationAcquisitionStrategy.shouldUseLastKnown(LocationResult.LowAccuracy(500f)))
    }

    @Test
    fun `shouldUseLastKnown - Timeout ergibt false`() {
        assertFalse(LocationAcquisitionStrategy.shouldUseLastKnown(LocationResult.Timeout))
    }

    @Test
    fun `shouldUseLastKnown - Unavailable ergibt false`() {
        assertFalse(LocationAcquisitionStrategy.shouldUseLastKnown(LocationResult.Unavailable))
    }

    @Test
    fun `shouldUseLastKnown - null ergibt false`() {
        assertFalse(LocationAcquisitionStrategy.shouldUseLastKnown(null))
    }

    // -------------------------------------------------------------------------
    // shouldTryHighAccuracy
    // -------------------------------------------------------------------------

    @Test
    fun `shouldTryHighAccuracy - gute Genauigkeit (1200m) ergibt false`() {
        val result = LocationResult.Success(52.0, 13.0, 1200f)
        assertFalse(LocationAcquisitionStrategy.shouldTryHighAccuracy(result))
    }

    @Test
    fun `shouldTryHighAccuracy - genau Grenzwert 1200m ergibt false`() {
        val result = LocationResult.Success(52.0, 13.0, 1200f)
        assertFalse(LocationAcquisitionStrategy.shouldTryHighAccuracy(result))
    }

    @Test
    fun `shouldTryHighAccuracy - 1201m (knapp ueber Grenze) ergibt true`() {
        val result = LocationResult.Success(52.0, 13.0, 1201f)
        assertTrue(LocationAcquisitionStrategy.shouldTryHighAccuracy(result))
    }

    @Test
    fun `shouldTryHighAccuracy - schlechte Genauigkeit (3000m) ergibt true`() {
        val result = LocationResult.Success(52.0, 13.0, 3000f)
        assertTrue(LocationAcquisitionStrategy.shouldTryHighAccuracy(result))
    }

    @Test
    fun `shouldTryHighAccuracy - LowAccuracy ergibt true`() {
        assertTrue(LocationAcquisitionStrategy.shouldTryHighAccuracy(LocationResult.LowAccuracy(5000f)))
    }

    @Test
    fun `shouldTryHighAccuracy - Timeout ergibt true`() {
        assertTrue(LocationAcquisitionStrategy.shouldTryHighAccuracy(LocationResult.Timeout))
    }

    @Test
    fun `shouldTryHighAccuracy - Unavailable ergibt true`() {
        assertTrue(LocationAcquisitionStrategy.shouldTryHighAccuracy(LocationResult.Unavailable))
    }

    @Test
    fun `shouldTryHighAccuracy - SkippedByUser ergibt false`() {
        assertFalse(LocationAcquisitionStrategy.shouldTryHighAccuracy(LocationResult.SkippedByUser))
    }

    // -------------------------------------------------------------------------
    // hasEnoughTimeForStageTwo
    // -------------------------------------------------------------------------

    @Test
    fun `hasEnoughTimeForStageTwo - genau 3000ms ergibt true`() {
        assertTrue(LocationAcquisitionStrategy.hasEnoughTimeForStageTwo(3000L))
    }

    @Test
    fun `hasEnoughTimeForStageTwo - 2999ms (knapp unter Grenze) ergibt false`() {
        assertFalse(LocationAcquisitionStrategy.hasEnoughTimeForStageTwo(2999L))
    }

    @Test
    fun `hasEnoughTimeForStageTwo - grosszuegiger Puffer ergibt true`() {
        assertTrue(LocationAcquisitionStrategy.hasEnoughTimeForStageTwo(10000L))
    }

    @Test
    fun `hasEnoughTimeForStageTwo - kein Puffer ergibt false`() {
        assertFalse(LocationAcquisitionStrategy.hasEnoughTimeForStageTwo(0L))
    }

    // -------------------------------------------------------------------------
    // chooseBetterResult
    // -------------------------------------------------------------------------

    @Test
    fun `chooseBetterResult - Success vs Timeout ergibt Success`() {
        val success = LocationResult.Success(52.0, 13.0, 100f)
        assertEquals(success, LocationAcquisitionStrategy.chooseBetterResult(success, LocationResult.Timeout))
    }

    @Test
    fun `chooseBetterResult - Timeout vs Success ergibt Success`() {
        val success = LocationResult.Success(52.0, 13.0, 100f)
        assertEquals(success, LocationAcquisitionStrategy.chooseBetterResult(LocationResult.Timeout, success))
    }

    @Test
    fun `chooseBetterResult - zwei LowAccuracy ergibt die genauere (niedrigerer Wert)`() {
        val better = LocationResult.LowAccuracy(1000f)
        val worse  = LocationResult.LowAccuracy(2000f)
        assertEquals(better, LocationAcquisitionStrategy.chooseBetterResult(worse, better))
    }

    @Test
    fun `chooseBetterResult - LowAccuracy Stage2 gegen Timeout Stage1 ergibt LowAccuracy`() {
        val lowAccuracy = LocationResult.LowAccuracy(5000f)
        val result = LocationAcquisitionStrategy.chooseBetterResult(LocationResult.Timeout, lowAccuracy)
        assertEquals(lowAccuracy, result)
    }

    @Test
    fun `chooseBetterResult - LowAccuracy Stage1 gegen Timeout Stage2 ergibt LowAccuracy`() {
        val lowAccuracy = LocationResult.LowAccuracy(5000f)
        val result = LocationAcquisitionStrategy.chooseBetterResult(lowAccuracy, LocationResult.Timeout)
        assertEquals(lowAccuracy, result)
    }

    @Test
    fun `chooseBetterResult - Unavailable Stage1 bleibt Unavailable gemaess Logik`() {
        // chooseBetterResult: wenn stageOne == Unavailable → return stageOne
        val result = LocationAcquisitionStrategy.chooseBetterResult(
            LocationResult.Unavailable,
            LocationResult.Timeout
        )
        assertEquals(LocationResult.Unavailable, result)
    }

    @Test
    fun `chooseBetterResult - beide Timeout ergibt Timeout (letzter Fallback)`() {
        val result = LocationAcquisitionStrategy.chooseBetterResult(
            LocationResult.Timeout,
            LocationResult.Timeout
        )
        assertEquals(LocationResult.Timeout, result)
    }

    // -------------------------------------------------------------------------
    // toLocationResult
    // -------------------------------------------------------------------------

    @Test
    fun `toLocationResult - gute Genauigkeit (100m) ergibt Success`() {
        val result = LocationAcquisitionStrategy.toLocationResult(52.0, 13.0, 100f)
        assertTrue(result is LocationResult.Success)
        val success = result as LocationResult.Success
        assertEquals(52.0, success.lat, 0.0001)
        assertEquals(13.0, success.lon, 0.0001)
        assertEquals(100f, success.accuracyMeters, 0.1f)
    }

    @Test
    fun `toLocationResult - genau Grenzwert 3000m ergibt Success`() {
        val result = LocationAcquisitionStrategy.toLocationResult(52.0, 13.0, 3000f)
        assertTrue(result is LocationResult.Success)
    }

    @Test
    fun `toLocationResult - 3001m (knapp ueber Grenze) ergibt LowAccuracy`() {
        val result = LocationAcquisitionStrategy.toLocationResult(52.0, 13.0, 3001f)
        assertTrue(result is LocationResult.LowAccuracy)
        assertEquals(3001f, (result as LocationResult.LowAccuracy).accuracyMeters, 0.1f)
    }

    @Test
    fun `toLocationResult - sehr schlechte Genauigkeit (10000m) ergibt LowAccuracy`() {
        val result = LocationAcquisitionStrategy.toLocationResult(52.0, 13.0, 10000f)
        assertTrue(result is LocationResult.LowAccuracy)
    }
}
