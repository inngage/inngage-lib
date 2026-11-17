package br.com.inngage.sdk.internal.service.location

import br.com.inngage.sdk.internal.service.location.domain.Coordinates
import br.com.inngage.sdk.internal.service.location.domain.GeoDistance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("GeoDistance")
class GeoDistanceTest {

    @Test
    fun `same point is zero meters`() {
        val p = Coordinates(-23.5505, -46.6333)
        assertEquals(0.0, GeoDistance.metersBetween(p, p), 1e-6)
    }

    @Test
    fun `São Paulo to Rio is about 360 km`() {
        val sp  = Coordinates(-23.5505, -46.6333)
        val rio = Coordinates(-22.9068, -43.1729)
        val d = GeoDistance.metersBetween(sp, rio)
        assertTrue(d in 355_000.0..365_000.0, "was $d")
    }

    @Test
    fun `one thousandth of a degree of latitude is about 111 m`() {
        val a = Coordinates(-23.5505, -46.6333)
        val b = Coordinates(-23.5515, -46.6333)
        val d = GeoDistance.metersBetween(a, b)
        assertTrue(d in 110.0..112.0, "was $d")
    }

    @Test
    fun `is symmetric`() {
        val a = Coordinates(10.0, 20.0)
        val b = Coordinates(11.0, 21.0)
        assertEquals(GeoDistance.metersBetween(a, b), GeoDistance.metersBetween(b, a), 1e-6)
    }

    @Test
    fun `coordinates validity follows API ranges`() {
        assertTrue(Coordinates(0.0, 0.0).isValid)
        assertTrue(Coordinates(90.0, 180.0).isValid)
        assertTrue(Coordinates(-90.0, -180.0).isValid)
        assertFalse(Coordinates(90.1, 0.0).isValid)
        assertFalse(Coordinates(0.0, -180.1).isValid)
    }
}
