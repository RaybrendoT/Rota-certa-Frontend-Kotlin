package com.rotacerta.mobile.core.location

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object Geo {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    /** Velocidade mínima (m/s) para considerar o ônibus em movimento ao estimar chegada. */
    private const val MIN_MOVING_SPEED = 1.5

    /** Distância em metros entre dois pontos (haversine), igual ao backend (core/geo.py). */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return EARTH_RADIUS_METERS * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    /** "850 m" ou "2,4 km". */
    fun formatDistance(meters: Double): String =
        if (meters < 1000) "${(meters / 10).roundToInt() * 10} m"
        else String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.1f km", meters / 1000)

    /**
     * Minutos estimados até a parada pela velocidade atual do ônibus (m/s).
     * Null quando parado/sem velocidade: melhor não mostrar do que mostrar um chute.
     */
    fun etaMinutes(distanceMeters: Double, speedMetersPerSecond: Double?): Int? {
        if (speedMetersPerSecond == null || speedMetersPerSecond < MIN_MOVING_SPEED) return null
        return maxOf(1, (distanceMeters / speedMetersPerSecond / 60).roundToInt())
    }
}
