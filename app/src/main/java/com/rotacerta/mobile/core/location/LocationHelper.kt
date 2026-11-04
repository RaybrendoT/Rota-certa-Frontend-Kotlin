package com.rotacerta.mobile.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

data class GeoPoint(val latitude: Double, val longitude: Double)

/**
 * Uma leitura do GPS: posição, direção (graus), velocidade (m/s) e precisão (m), quando o aparelho
 * informa. [isMock] indica posição vinda de um app de localização simulada.
 */
data class LocationFix(
    val point: GeoPoint,
    val heading: Float? = null,
    val speed: Float? = null,
    val accuracy: Float? = null,
    val isMock: Boolean = false,
)

/** GPS do aparelho (Fused Location Provider). */
@Singleton
class LocationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun hasPermission(): Boolean = LOCATION_PERMISSIONS.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    /** Posição atual com alta precisão, ou null sem permissão/sinal. */
    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): GeoPoint? {
        if (!hasPermission()) return null
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(30_000)
            .build()
        return runCatching {
            LocationServices.getFusedLocationProviderClient(context)
                .getCurrentLocation(request, null)
                .await()
                ?.let { GeoPoint(it.latitude, it.longitude) }
        }.getOrNull()
    }

    /**
     * Leituras contínuas do GPS enquanto houver coletor (ex.: a tela de navegação aberta).
     * Sem permissão, o fluxo termina sem emitir nada.
     */
    @SuppressLint("MissingPermission")
    fun locationUpdates(intervalMillis: Long = 2_000): Flow<LocationFix> = callbackFlow {
        if (!hasPermission()) {
            close()
            return@callbackFlow
        }
        val client = LocationServices.getFusedLocationProviderClient(context)
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    trySend(
                        LocationFix(
                            point = GeoPoint(location.latitude, location.longitude),
                            heading = if (location.hasBearing()) location.bearing else null,
                            speed = if (location.hasSpeed()) location.speed else null,
                            accuracy = if (location.hasAccuracy()) location.accuracy else null,
                            isMock = location.isMockLocation(),
                        ),
                    )
                }
            }
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis).build()
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { client.removeLocationUpdates(callback) }
    }

    /**
     * Leitura nova (nunca a última guardada) até a precisão chegar a [maxAccuracyMeters], esperando no
     * máximo [timeoutMillis]. Devolve a leitura mais precisa obtida, mesmo que não tenha chegado lá,
     * ou null sem permissão/sinal.
     */
    suspend fun preciseFix(maxAccuracyMeters: Float, timeoutMillis: Long = 20_000): LocationFix? {
        var best: LocationFix? = null
        withTimeoutOrNull(timeoutMillis) {
            locationUpdates(intervalMillis = 1_000).first { fix ->
                val accuracy = fix.accuracy ?: Float.MAX_VALUE
                if (accuracy < (best?.accuracy ?: Float.MAX_VALUE)) best = fix
                accuracy <= maxAccuracyMeters
            }
        }
        return best
    }

    companion object {
        val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }
}

/** Posição vinda de um app de localização simulada (isMock no Android 12+). */
private fun Location.isMockLocation(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) isMock else @Suppress("DEPRECATION") isFromMockProvider
