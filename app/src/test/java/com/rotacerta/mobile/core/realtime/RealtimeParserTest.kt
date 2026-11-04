package com.rotacerta.mobile.core.realtime

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RealtimeParserTest {
    // Mesma configuração do NetworkModule.
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        isLenient = true
    }

    @Test
    fun `connected traz o person_id`() {
        val event = RealtimeParser.parse(json, """{"type":"connected","person_id":7}""")
        assertEquals(RealtimeEvent.Connected(7), event)
    }

    @Test
    fun `notification decodifica o aviso aninhado`() {
        val event = RealtimeParser.parse(
            json,
            """{"type":"notification","notification":{"id":10,"kind":"route_delay","title":"Atraso na rota Rota A",
               "message":"Trânsito intenso","route_id":3,"data":{},"created_at":"2026-10-27T11:12:00Z","read":false}}""",
        ) as RealtimeEvent.NotificationReceived
        assertEquals(10L, event.notification.id)
        assertEquals("route_delay", event.notification.kind)
        assertEquals(3L, event.notification.routeId)
        assertEquals("Trânsito intenso", event.notification.message)
    }

    @Test
    fun `vehicle_location aceita heading e speed nulos`() {
        val event = RealtimeParser.parse(
            json,
            """{"type":"vehicle_location","route_id":3,"latitude":-3.095,"longitude":-60.0,
               "heading":null,"speed":null,"recorded_at":"2026-10-27T11:12:05+00:00"}""",
        ) as RealtimeEvent.VehicleLocation
        assertEquals(3L, event.location.routeId)
        assertEquals(-3.095, event.location.latitude, 0.0)
        assertNull(event.location.speed)
    }

    @Test
    fun `boarding_status e route_event`() {
        assertEquals(
            RealtimeEvent.BoardingStatusChanged(routeId = 3, routePassengerId = 21, personId = 12, status = "confirmed"),
            RealtimeParser.parse(json, """{"type":"boarding_status","route_id":3,"route_passenger_id":21,"person_id":12,"status":"confirmed"}"""),
        )
        assertEquals(
            RealtimeEvent.RouteEventOccurred(routeId = 3, eventType = "departure", description = "", routeStatus = "in_route"),
            RealtimeParser.parse(
                json,
                """{"type":"route_event","route_id":3,"event_type":"departure","description":"","route_status":"in_route","timestamp":"x"}""",
            ),
        )
    }

    @Test
    fun `pong, tipos desconhecidos e JSON invalido sao ignorados`() {
        assertNull(RealtimeParser.parse(json, """{"type":"pong"}"""))
        assertNull(RealtimeParser.parse(json, """{"type":"novidade","x":1}"""))
        assertNull(RealtimeParser.parse(json, "isto não é json"))
        assertNull(RealtimeParser.parse(json, """{"type":"vehicle_location"}"""))
        assertNull(RealtimeParser.parse(json, "[1,2]"))
    }

    @Test
    fun `backoff dobra ate 30 segundos`() {
        assertEquals(1_000L, RealtimeClient.backoffMillis(0))
        assertEquals(2_000L, RealtimeClient.backoffMillis(1))
        assertEquals(16_000L, RealtimeClient.backoffMillis(4))
        assertEquals(30_000L, RealtimeClient.backoffMillis(5))
        assertTrue(RealtimeClient.backoffMillis(50) <= 30_000L)
    }
}
