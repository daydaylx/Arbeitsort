package de.montagezeit.app.domain.location

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

class LocationCalculatorTest {
    
    private val calculator = LocationCalculator()
    
    @Test
    fun `calculateDistanceToLeipzig - Leipzig Zentrum`() {
        // Leipzig Zentrum zu sich selbst
        val distance = calculator.calculateDistanceToLeipzig(51.340, 12.374)
        assertTrue("Distanz sollte < 100m sein", distance < 100)
    }
    
    @Test
    fun `calculateDistance - Bekannte Distanz`() {
        // Leipzig (51.340, 12.374) zu Dresden (51.050, 13.737)
        // Erwartete Distanz ca. 100km
        val distance = calculator.calculateDistance(51.340, 12.374, 51.050, 13.737)
        val distanceKm = distance / 1000.0
        assertTrue("Distanz Leipzig-Dresden sollte ca. 100km sein, war $distanceKm", distanceKm in 95.0..105.0)
    }
    
    @Test
    fun `checkLeipzigLocation - Innerhalb 30km`() {
        // Punkt ca. 10km von Leipzig Zentrum
        val result = calculator.checkLeipzigLocation(51.400, 12.450, 30.0)
        
        assertTrue("Sollte innerhalb sein", result.isInside == true)
        assertFalse("Sollte keine Bestätigung benötigen", result.confirmRequired)
        assertTrue("Distanz sollte < 30km sein", result.distanceKm < 30.0)
    }
    
    @Test
    fun `checkLeipzigLocation - Außerhalb 32km`() {
        // Punkt ca. 40km von Leipzig Zentrum
        val result = calculator.checkLeipzigLocation(51.100, 12.800, 30.0)
        
        assertTrue("Sollte außerhalb sein", result.isInside == false)
        assertFalse("Sollte keine Bestätigung benötigen", result.confirmRequired)
        assertTrue("Distanz sollte > 32km sein", result.distanceKm > 32.0)
    }
    
    @Test
    fun `checkLeipzigLocation - Grenzzone 29km`() {
        // Punkt in Grenzzone (28-32km)
        // Dresden ist ca. 100km, wir brauchen etwas näheres
        // Test mit Punkt ca. 29km von Leipzig
        val result = calculator.checkLeipzigLocation(51.580, 12.200, 30.0)
        
        assertNull("Sollte in Grenzzone sein (null)", result.isInside)
        assertTrue("Sollte Bestätigung benötigen", result.confirmRequired)
        assertTrue("Sollte in Grenzzone sein", result.distanceKm in 28.0..32.0)
    }
    
    @Test
    fun `checkLeipzigLocation - Grenzzone 31km`() {
        // Punkt in Grenzzone (28-32km) am oberen Rand
        // 51.619 ist ca. 31km nördlich von 51.340
        val result = calculator.checkLeipzigLocation(51.619, 12.374, 30.0)
        
        assertNull("Sollte in Grenzzone sein (null)", result.isInside)
        assertTrue("Sollte Bestätigung benötigen", result.confirmRequired)
        assertTrue("Sollte in Grenzzone sein", result.distanceKm in 28.0..32.0)
    }
    
    @Test
    fun `isAccuracyAcceptable - Gute Genauigkeit`() {
        assertTrue("100m sollte akzeptabel sein", calculator.isAccuracyAcceptable(100.0f))
        assertTrue("1000m sollte akzeptabel sein", calculator.isAccuracyAcceptable(1000.0f))
        assertTrue("3000m sollte akzeptabel sein", calculator.isAccuracyAcceptable(3000.0f))
    }
    
    @Test
    fun `isAccuracyAcceptable - Schlechte Genauigkeit`() {
        assertFalse("3001m sollte nicht akzeptabel sein", calculator.isAccuracyAcceptable(3001.0f))
        assertFalse("5000m sollte nicht akzeptabel sein", calculator.isAccuracyAcceptable(5000.0f))
    }
    
    @Test
    fun `DEFAULT_RADIUS_KM sollte 30 sein`() {
        assertEquals(30.0, LocationCalculator.DEFAULT_RADIUS_KM, 0.0)
    }
}