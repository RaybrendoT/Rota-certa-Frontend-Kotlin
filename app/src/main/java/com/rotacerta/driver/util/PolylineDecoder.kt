package com.rotacerta.driver.util

import com.google.android.gms.maps.model.LatLng

/**
 * Decodifica uma polyline codificada no formato padrão do Google
 * (https://developers.google.com/maps/documentation/utilities/polylinealgorithm)
 * em uma lista de pontos LatLng, para desenhar a rota no mapa.
 */
object PolylineDecoder {

    fun decode(encoded: String): List<LatLng> {
        if (encoded.isBlank()) return emptyList()

        val points = mutableListOf<LatLng>()
        var index = 0
        var lat = 0
        var lng = 0

        while (index < encoded.length) {
            var shift = 0
            var result = 0
            var b: Int
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val deltaLat = if (result and 1 != 0) (result shr 1).inv() else (result shr 1)
            lat += deltaLat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val deltaLng = if (result and 1 != 0) (result shr 1).inv() else (result shr 1)
            lng += deltaLng

            points.add(LatLng(lat / 1e5, lng / 1e5))
        }
        return points
    }
}
