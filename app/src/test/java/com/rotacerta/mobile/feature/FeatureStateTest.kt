package com.rotacerta.mobile.feature

import com.rotacerta.mobile.core.util.UiState
import com.rotacerta.mobile.data.dto.CollaboratorRouteDto
import com.rotacerta.mobile.data.dto.CollaboratorRouteInfoDto
import com.rotacerta.mobile.data.dto.CollaboratorStopDto
import com.rotacerta.mobile.data.dto.RouteEventDto
import com.rotacerta.mobile.data.dto.VehicleLocationDto
import com.rotacerta.mobile.feature.collaborator.CollaboratorUiState
import com.rotacerta.mobile.feature.driver.detail.RouteDetailUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CollaboratorProximityTest {
    private fun route(location: VehicleLocationDto?, hasStop: Boolean = true) = CollaboratorRouteDto(
        id = 21,
        boardingStatus = "pending",
        route = CollaboratorRouteInfoDto(id = 3, name = "Rota A", companyName = "Acme"),
        stop = if (hasStop) CollaboratorStopDto(9, 0, "Ponto A", "Rua X", -3.100, -60.0) else null,
        lastLocation = location,
    )

    private fun bus(speed: Double?) = VehicleLocationDto(3, -3.095, -60.0, null, speed, "2026-10-27T11:00:00Z")

    @Test
    fun `distancia e chegada estimada do onibus ate o ponto`() {
        val r = route(bus(speed = 9.0))
        val proximity = CollaboratorUiState(routes = UiState.Success(listOf(r)), selected = r).proximity!!
        assertEquals(556.0, proximity.distanceMeters, 2.0)
        assertEquals(1, proximity.etaMinutes)
    }

    @Test
    fun `sem posicao, sem ponto ou parado`() {
        assertNull(CollaboratorUiState(selected = route(null)).proximity)
        assertNull(CollaboratorUiState(selected = route(bus(9.0), hasStop = false)).proximity)
        assertNull(CollaboratorUiState(selected = route(bus(speed = 0.0))).proximity!!.etaMinutes)
    }
}

class TripInProgressTest {
    private fun event(id: Long, type: String) = RouteEventDto(id, 3, type, "", -3.1, -60.0, "2026-10-27T11:00:00Z")

    @Test
    fun `partida mais recente que a chegada`() {
        // Lista do backend: mais novo primeiro.
        assertTrue(RouteDetailUiState(events = listOf(event(3, "stop"), event(2, "departure"), event(1, "arrival"))).tripInProgress)
        assertFalse(RouteDetailUiState(events = listOf(event(3, "arrival"), event(2, "stop"), event(1, "departure"))).tripInProgress)
        assertFalse(RouteDetailUiState(events = emptyList()).tripInProgress)
    }
}
