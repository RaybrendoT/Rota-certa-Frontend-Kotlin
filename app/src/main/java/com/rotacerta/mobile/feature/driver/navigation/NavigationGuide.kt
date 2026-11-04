package com.rotacerta.mobile.feature.driver.navigation

import com.rotacerta.mobile.core.location.Geo
import com.rotacerta.mobile.core.location.GeoPoint
import com.rotacerta.mobile.data.dto.NavigationResponseDto
import com.rotacerta.mobile.data.dto.NavigationStepDto
import com.rotacerta.mobile.data.dto.NavigationStopDto
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Onde o motorista está em relação ao trajeto calculado. */
data class NavigationProgress(
    /** Metros percorridos desde o início do trajeto (projeção do GPS na linha). */
    val alongMeters: Double,
    /** Distância do GPS até a linha do trajeto. */
    val offRouteMeters: Double,
    /** Segmento da polyline (entre os pontos i e i+1) onde o GPS foi projetado. */
    val segmentIndex: Int,
    /** Próxima manobra (null = só seguir até o destino). */
    val nextStep: NavigationStepDto?,
    val metersToNextStep: Double,
    /** Manobra depois da próxima, para o "Depois: ...". */
    val followingStep: NavigationStepDto?,
    /** Próxima parada pendente (null = próximo ponto é o destino). */
    val nextStop: NavigationStopDto?,
    val metersToNextStop: Double,
    val secondsToNextStop: Int,
    val remainingMeters: Double,
    val remainingSeconds: Int,
    /** GPS a menos de [ARRIVING_RADIUS_METERS] da próxima parada. */
    val arrivingAtStop: Boolean,
) {
    val isOffRoute: Boolean get() = offRouteMeters > OFF_ROUTE_METERS

    companion object {
        const val OFF_ROUTE_METERS = 60.0
        const val ARRIVING_RADIUS_METERS = 150.0
    }
}

/**
 * Acompanha o motorista ao longo do trajeto devolvido pelo backend, sem chamar a Routes API de novo:
 * cada leitura do GPS é projetada na polyline e convertida em manobra/parada/tempo restantes.
 */
class NavigationGuide(
    private val navigation: NavigationResponseDto,
    polyline: List<GeoPoint>,
) {
    private val track = Track(
        polyline.ifEmpty {
            listOf(navigation.waypoints.origin.toGeo()) +
                navigation.waypoints.intermediates.map { it.toGeo() } +
                navigation.waypoints.destination.toGeo()
        },
    )

    private val steps = navigation.steps

    /** Onde cada passo começa na linha (a manobra do passo acontece no início dele). */
    private val stepStartAlong: DoubleArray

    /** Onde cada trecho termina na linha (trecho i termina na parada legs[i].stopId ou no destino). */
    private val legEndAlong: DoubleArray

    private val stopsById = navigation.stops.associateBy { it.id }

    val totalMeters: Double get() = track.total

    init {
        // Projeção monotônica: um passo nunca começa antes do anterior (a rota pode cruzar a mesma rua duas vezes).
        var segmentFloor = 0
        stepStartAlong = DoubleArray(steps.size) { index ->
            val projection = track.project(steps[index].start.toGeo(), fromSegment = segmentFloor)
            segmentFloor = projection.segment
            projection.along
        }
        val legs = navigation.legs
        legEndAlong = DoubleArray(legs.size) { legIndex ->
            val lastStep = steps.indexOfLast { it.legIndex == legIndex }
            when {
                legIndex == legs.lastIndex -> track.total
                lastStep >= 0 && lastStep + 1 < steps.size -> stepStartAlong[lastStep + 1]
                else -> track.total * (legIndex + 1) / legs.size
            }
        }
    }

    /** [previousAlong]: posição anterior na linha, para não "pular" para outro trecho que passa no mesmo lugar. */
    fun progress(position: GeoPoint, previousAlong: Double? = null): NavigationProgress {
        val projection = previousAlong
            ?.let { track.projectNear(position, it) }
            ?.takeIf { it.distance <= NavigationProgress.OFF_ROUTE_METERS }
            ?: track.project(position)
        val along = projection.along

        val nextStepIndex = stepStartAlong.indexOfFirst { it > along + STEP_PASSED_METERS }
        val nextStep = steps.getOrNull(nextStepIndex)
        val metersToNextStep = if (nextStepIndex >= 0) stepStartAlong[nextStepIndex] - along else track.total - along

        val legIndex = legEndAlong.indexOfFirst { it > along }.takeIf { it >= 0 } ?: navigation.legs.lastIndex
        val leg = navigation.legs.getOrNull(legIndex)
        val nextStop = leg?.stopId?.let(stopsById::get)
        val legEnd = legEndAlong.getOrNull(legIndex) ?: track.total
        val metersToLegEnd = (legEnd - along).coerceAtLeast(0.0)

        return NavigationProgress(
            alongMeters = along,
            offRouteMeters = projection.distance,
            segmentIndex = projection.segment,
            nextStep = nextStep,
            metersToNextStep = metersToNextStep.coerceAtLeast(0.0),
            followingStep = if (nextStepIndex >= 0) steps.getOrNull(nextStepIndex + 1) else null,
            nextStop = nextStop,
            metersToNextStop = metersToLegEnd,
            secondsToNextStop = legSecondsRemaining(legIndex, metersToLegEnd),
            remainingMeters = (track.total - along).coerceAtLeast(0.0),
            remainingSeconds = legSecondsRemaining(legIndex, metersToLegEnd) +
                navigation.legs.drop(legIndex + 1).sumOf { it.durationSeconds },
            arrivingAtStop = nextStop != null &&
                Geo.distanceMeters(position.latitude, position.longitude, nextStop.latitude, nextStop.longitude) <=
                NavigationProgress.ARRIVING_RADIUS_METERS,
        )
    }

    /** Tempo do trecho atual proporcional ao que falta dele (a Routes API já considera o trânsito). */
    private fun legSecondsRemaining(legIndex: Int, metersToLegEnd: Double): Int {
        val leg = navigation.legs.getOrNull(legIndex) ?: return 0
        val legStart = legEndAlong.getOrNull(legIndex - 1) ?: 0.0
        val legLength = (legEndAlong[legIndex] - legStart).takeIf { it > 1 } ?: return leg.durationSeconds
        return (leg.durationSeconds * (metersToLegEnd / legLength).coerceIn(0.0, 1.0)).roundToInt()
    }

    private companion object {
        /** Manobra considerada feita quando o GPS passou poucos metros do ponto dela. */
        const val STEP_PASSED_METERS = 10.0
    }
}

private fun com.rotacerta.mobile.data.dto.LatLngDto.toGeo() = GeoPoint(latitude, longitude)

/** Polyline com distâncias acumuladas e projeção de pontos (aproximação plana local, boa para poucos km). */
internal class Track(private val points: List<GeoPoint>) {
    private val cumulative = DoubleArray(points.size).also { acc ->
        for (i in 1 until points.size) {
            acc[i] = acc[i - 1] + Geo.distanceMeters(
                points[i - 1].latitude, points[i - 1].longitude, points[i].latitude, points[i].longitude,
            )
        }
    }

    val total: Double get() = cumulative.lastOrNull() ?: 0.0

    data class Projection(val segment: Int, val along: Double, val distance: Double)

    /** Ponto da linha mais próximo, procurando a partir do segmento [fromSegment]. */
    fun project(point: GeoPoint, fromSegment: Int = 0): Projection =
        projectRange(point, fromSegment, points.size - 2)

    /** Procura só perto de [along] (um pouco para trás, bastante para frente). */
    fun projectNear(point: GeoPoint, along: Double): Projection {
        val from = segmentAt(along - NEAR_BACK_METERS)
        val to = segmentAt(along + NEAR_AHEAD_METERS)
        return projectRange(point, from, to)
    }

    private fun segmentAt(along: Double): Int {
        val index = cumulative.indexOfFirst { it > along }
        return when {
            index < 0 -> (points.size - 2).coerceAtLeast(0)
            else -> (index - 1).coerceAtLeast(0)
        }
    }

    private fun projectRange(point: GeoPoint, from: Int, to: Int): Projection {
        if (points.size < 2) {
            val only = points.firstOrNull() ?: return Projection(0, 0.0, 0.0)
            return Projection(0, 0.0, Geo.distanceMeters(point.latitude, point.longitude, only.latitude, only.longitude))
        }
        var best = Projection(from, cumulative[from], Double.MAX_VALUE)
        for (i in from.coerceAtLeast(0)..to.coerceAtMost(points.size - 2)) {
            val (t, distance) = projectOnSegment(point, points[i], points[i + 1])
            if (distance < best.distance) {
                best = Projection(i, cumulative[i] + t * (cumulative[i + 1] - cumulative[i]), distance)
            }
        }
        return best
    }

    /** Fração t (0..1) do segmento a-b mais próxima de p e a distância em metros até ela. */
    private fun projectOnSegment(p: GeoPoint, a: GeoPoint, b: GeoPoint): Pair<Double, Double> {
        val metersPerDegLat = 111_320.0
        val metersPerDegLng = metersPerDegLat * cos(Math.toRadians(p.latitude))
        val ax = (a.longitude - p.longitude) * metersPerDegLng
        val ay = (a.latitude - p.latitude) * metersPerDegLat
        val bx = (b.longitude - p.longitude) * metersPerDegLng
        val by = (b.latitude - p.latitude) * metersPerDegLat
        val dx = bx - ax
        val dy = by - ay
        val lengthSquared = dx * dx + dy * dy
        val t = if (lengthSquared == 0.0) 0.0 else (-(ax * dx + ay * dy) / lengthSquared).coerceIn(0.0, 1.0)
        val x = ax + t * dx
        val y = ay + t * dy
        return t to sqrt(x * x + y * y)
    }

    private companion object {
        const val NEAR_BACK_METERS = 150.0
        const val NEAR_AHEAD_METERS = 3_000.0
    }
}
