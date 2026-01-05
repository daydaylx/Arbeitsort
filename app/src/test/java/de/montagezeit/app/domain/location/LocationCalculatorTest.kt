package de.montagezeit.app.domain.location

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocationCalculatorTest {
    
    private val calculator = LocationCalculator()
    
    @Test
    fun `calculateDistanceToLeipzig - Leipzig Zentrum`() {
        // Leipzig Zentrum zu sich selbst
        val distance = calculator.calculateDistanceToLeipzig(51.340, 12.374)
        assertTrue(distance < 100, "Distanz sollte < 100m sein")
    }
    
    @Test
    fun `calculateDistance - Bekannte Distanz`() {
        // Leipzig (51.340, 12.374) zu Dresden (51.050, 13.737)
        // Erwartete Distanz ca. 100km
        val distance = calculator.calculateDistance(51.340, 12.374, 51.050, 13.737)
        val distanceKm = distance / 1000.0
        assertTrue(distanceKm in 95.0..105.0, "Distanz Leipzig-Dresden sollte ca. 100km sein, war $distanceKm")
    }
    
    @Test
    fun `checkLeipzigLocation - Innerhalb 30km`() {
        // Punkt ca. 10km von Leipzig Zentrum
        val result = calculator.checkLeipzigLocation(51.400, 12.450, 30.0)
        
        assertTrue(result.isInside == true, "Sollte innerhalb sein")
        assertFalse(result.confirmRequired, "Sollte keine Bestätigung benötigen")
        assertTrue(result.distanceKm < 30.0, "Distanz sollte < 30km sein")
    }
    
    @Test
    fun `checkLeipzigLocation - Außerhalb 32km`() {
        // Punkt ca. 40km von Leipzig Zentrum
        val result = calculator.checkLeipzigLocation(51.100, 12.800, 30.0)
        
        assertTrue(result.isInside == false, "Sollte außerhalb sein")
        assertFalse(result.confirmRequired, "Sollte keine Bestätigung benötigen")
        assertTrue(result.distanceKm > 32.0, "Distanz sollte > 32km sein")
    }
    
    @Test
    fun `checkLeipzigLocation - Grenzzone 29km`() {
        // Punkt in Grenzzone (28-32km)
        // Dresden ist ca. 100km, wir brauchen etwas näheres
        // Test mit Punkt ca. 29km von Leipzig
        val result = calculator.checkLeipzigLocation(51.580, 12.200, 30.0)
        
        assertNull(result.isInside, "Sollte in Grenzzone sein (null)")
        assertTrue(result.confirmRequired, "Sollte Bestätigung benötigen")
        assertTrue(result.distanceKm in 28.0..32.0, "Sollte in Grenzzone sein")
    }
    
    @Test
    fun `checkLeipzigLocation - Grenzzone 31km`() {
        // Punkt in Grenzzone (28-32km) am oberen Rand
        val result = calculator.checkLeipzigLocation(51.020, 12.100, 30.0)
        
        assertNull(result.isInside, "Sollte in Grenzzone sein (null)")
        assertTrue(result.confirmRequired, "Sollte Bestätigung benötigen")
        assertTrue(result.distanceKm in 28.0..32.0, "Sollte in Grenzzone sein")
    }
    
    @Test
    fun `isAccuracyAcceptable - Gute Genauigkeit`() {
        assertTrue(calculator.isAccuracyAcceptable(100.0f), "100m sollte akzeptabel sein")
        assertTrue(calculator.isAccuracyAcceptable(1000.0f), "1000m sollte akzeptabel sein")
        assertTrue(calculator.isAccuracyAcceptable(3000.0f), "3000m sollte akzeptabel sein")
    }
    
    @Test
    fun `isAccuracyAcceptable - Schlechte Genauigkeit`() {
        assertFalse(calculator.isAccuracyAcceptable(3001.0f), "3001m sollte nicht akzeptabel sein")
        assertFalse(calculator.isAccuracyAcceptable(5000.0f), "5000m sollte nicht akzeptabel sein")
    }
    
    @Test
    fun `DEFAULT_RADIUS_KM sollte 30 sein`() {
        assertEquals(30.0, LocationCalculator.DEFAULT_RADIUS_KM)
    }
}
