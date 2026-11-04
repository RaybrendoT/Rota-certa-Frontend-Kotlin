package com.rotacerta.mobile.feature

import com.rotacerta.mobile.core.location.GeoPoint
import com.rotacerta.mobile.data.dto.LatLngDto
import com.rotacerta.mobile.data.dto.NavigationLegDto
import com.rotacerta.mobile.data.dto.NavigationResponseDto
import com.rotacerta.mobile.data.dto.NavigationStepDto
import com.rotacerta.mobile.data.dto.NavigationStopDto
import com.rotacerta.mobile.data.dto.NavigationWaypointsDto
import com.rotacerta.mobile.feature.driver.TripProgress
import com.rotacerta.mobile.feature.driver.navigation.NavigationGuide
import com.rotacerta.mobile.data.dto.RouteEventDto
import com.rotacerta.mobile.data.dto.RouteStopDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 0,001° de latitude ≈ 111 m. */
private const val M_PER_MILLIDEGREE = 111.2

private fun lat(milli: Int) = -3.0 - milli / 1000.0

/** Linha reta para o sul, longitude -60, de -3,000 a -3,020 (≈ 2,2 km), um ponto a cada ~111 m. */
private val straightLine = (0..20).map { GeoPoint(lat(it), -60.0) }

private fun step(legIndex: Int, from: Int, to: Int, text: String, maneuver: String = "") = NavigationStepDto(
    legIndex = legIndex,
    instruction = text,
    maneuver = maneuver,
    distanceMeters = ((to - from) * M_PER_MILLIDEGREE).toInt(),
    start = LatLngDto(lat(from), -60.0),
    end = LatLngDto(lat(to), -60.0),
)

/** Parada 1 em -3,015; destino em -3,020. Trecho 0 até a parada (300 s), trecho 1 até o destino (200 s). */
private val navigation = NavigationResponseDto(
    route = 9,
    waypoints = NavigationWaypointsDto(
        origin = LatLngDto(lat(0), -60.0),
        intermediates = listOf(LatLngDto(lat(15), -60.0)),
        destination = LatLngDto(lat(20), -60.0),
    ),
    stops = listOf(
        NavigationStopDto(7, 0, "Ponto Feito", -3.5, -60.5, done = true),
        NavigationStopDto(1, 1, "Ponto Um", lat(15), -60.0),
    ),
    legs = listOf(NavigationLegDto(stopId = 1, durationSeconds = 300), NavigationLegDto(stopId = null, durationSeconds = 200)),
    steps = listOf(
        step(0, 0, 10, "Siga para o sul"),
        step(0, 10, 15, "Vire à direita", "TURN_RIGHT"),
        step(1, 15, 20, "Vire à esquerda", "TURN_LEFT"),
    ),
)

class NavigationGuideTest {
    private val guide = NavigationGuide(navigation, straightLine)

    private fun assertMeters(expected: Double, actual: Double) = assertEquals(expected, actual, 15.0)

    @Test
    fun `no inicio mostra a proxima manobra, a parada e o tempo total`() {
        val p = guide.progress(GeoPoint(lat(0), -60.0))
        assertEquals("Vire à direita", p.nextStep?.instruction)
        assertMeters(10 * M_PER_MILLIDEGREE, p.metersToNextStep)
        assertEquals("Vire à esquerda", p.followingStep?.instruction)
        assertEquals(1L, p.nextStop?.id)
        assertMeters(15 * M_PER_MILLIDEGREE, p.metersToNextStop)
        assertEquals(300, p.secondsToNextStop)
        assertEquals(500, p.remainingSeconds)
        assertMeters(20 * M_PER_MILLIDEGREE, p.remainingMeters)
        assertFalse(p.arrivingAtStop)
        assertFalse(p.isOffRoute)
    }

    @Test
    fun `no meio do trecho o tempo e proporcional ao que falta`() {
        val p = guide.progress(GeoPoint(lat(12), -60.0))
        assertEquals("Vire à esquerda", p.nextStep?.instruction)
        assertMeters(3 * M_PER_MILLIDEGREE, p.metersToNextStep)
        assertEquals(60, p.secondsToNextStop) // 3 de 15 partes de 300 s
        assertEquals(260, p.remainingSeconds)
        assertFalse(p.arrivingAtStop)
    }

    @Test
    fun `perto da parada avisa a chegada`() {
        assertTrue(guide.progress(GeoPoint(lat(14), -60.0)).arrivingAtStop)
    }

    @Test
    fun `depois da ultima manobra segue ate o destino`() {
        val p = guide.progress(GeoPoint(lat(17), -60.0))
        assertNull(p.nextStep)
        assertNull(p.nextStop)
        assertMeters(3 * M_PER_MILLIDEGREE, p.metersToNextStep)
        assertEquals(120, p.remainingSeconds) // 3 de 5 partes de 200 s
    }

    @Test
    fun `fora da linha marca desvio de rota`() {
        val p = guide.progress(GeoPoint(lat(5), -59.999)) // ~111 m a leste
        assertMeters(111.0, p.offRouteMeters)
        assertTrue(p.isOffRoute)
        assertMeters(5 * M_PER_MILLIDEGREE, p.alongMeters)
    }

    @Test
    fun `ida e volta pela mesma rua usa a posicao anterior`() {
        val outAndBack = (0..10).map { GeoPoint(lat(it), -60.0) } + (9 downTo 0).map { GeoPoint(lat(it), -60.0) }
        val loop = NavigationGuide(navigation.copy(steps = emptyList()), outAndBack)
        val point = GeoPoint(lat(5), -60.0)
        assertMeters(5 * M_PER_MILLIDEGREE, loop.progress(point).alongMeters)
        assertMeters(15 * M_PER_MILLIDEGREE, loop.progress(point, previousAlong = 14 * M_PER_MILLIDEGREE).alongMeters)
    }
}

class TripProgressTest {
    private fun event(type: String, latitude: Double = -3.1) =
        RouteEventDto(1, 9, type, "", latitude, -60.0, "2026-10-29T10:00:00Z")

    private val stops = listOf(
        RouteStopDto(1, 0, "A", "", -3.1, -60.0),
        RouteStopDto(2, 1, "B", "", -3.2, -60.0),
    )

    @Test
    fun `paradas feitas contam so depois da ultima partida`() {
        // Mais novo primeiro: parada em A na viagem atual; parada em B foi na viagem anterior.
        val events = listOf(event("stop", -3.1), event("departure"), event("arrival"), event("stop", -3.2), event("departure"))
        assertEquals(setOf(1L), TripProgress.completedStopIds(stops, events))
        assertTrue(TripProgress.inProgress(events))
    }

    @Test
    fun `viagem finalizada nao esta em andamento`() {
        assertFalse(TripProgress.inProgress(listOf(event("arrival"), event("departure"))))
        assertEquals(emptySet<Long>(), TripProgress.completedStopIds(stops, emptyList()))
    }
}
