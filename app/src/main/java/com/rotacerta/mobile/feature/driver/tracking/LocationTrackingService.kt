package com.rotacerta.mobile.feature.driver.tracking

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.rotacerta.mobile.R
import com.rotacerta.mobile.core.location.GeoPoint
import com.rotacerta.mobile.core.location.LocationHelper
import com.rotacerta.mobile.core.network.ApiResult
import com.rotacerta.mobile.core.notification.SystemNotifier
import com.rotacerta.mobile.data.dto.LocationUpdateRequest
import com.rotacerta.mobile.data.repository.DriverRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Envia o GPS do motorista ao backend (POST /routes/{id}/location/) enquanto a rota está em andamento.
 * Foreground service: continua com a tela desligada e mantém o WebSocket do app vivo.
 */
@AndroidEntryPoint
class LocationTrackingService : LifecycleService() {

    @Inject lateinit var driverRepository: DriverRepository
    @Inject lateinit var controller: TrackingController
    @Inject lateinit var locationHelper: LocationHelper

    private lateinit var fusedClient: FusedLocationProviderClient
    private var routeId: Long = NO_ROUTE
    private var sending = false

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let(::onLocation)
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            stopTracking()
            return START_NOT_STICKY
        }

        val newRouteId = intent?.getLongExtra(EXTRA_ROUTE_ID, NO_ROUTE) ?: NO_ROUTE
        val routeName = intent?.getStringExtra(EXTRA_ROUTE_NAME).orEmpty()
        if (newRouteId == NO_ROUTE || !locationHelper.hasPermission()) {
            stopTracking()
            return START_NOT_STICKY
        }

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(routeName),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0,
        )
        if (routeId != newRouteId) {
            routeId = newRouteId
            requestUpdates()
        }
        controller.onServiceStarted(routeId, routeName)
        // Se o sistema matar o processo, recria o serviço com a mesma rota.
        return START_REDELIVER_INTENT
    }

    @SuppressLint("MissingPermission") // checada em onStartCommand
    private fun requestUpdates() {
        fusedClient.removeLocationUpdates(callback)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
            .setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MS)
            .setMinUpdateDistanceMeters(MIN_DISTANCE_METERS)
            .build()
        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
    }

    private fun onLocation(location: Location) {
        val heading = if (location.hasBearing()) location.bearing else null
        controller.onLocation(GeoPoint(location.latitude, location.longitude), heading)
        if (sending) return // não empilha envios com a rede lenta; o próximo ponto substitui
        sending = true
        val currentRoute = routeId
        lifecycleScope.launch {
            val result = driverRepository.sendLocation(
                currentRoute,
                LocationUpdateRequest(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    heading = heading?.toDouble(),
                    speed = if (location.hasSpeed()) location.speed.toDouble() else null,
                    accuracy = if (location.hasAccuracy()) location.accuracy.toDouble() else null,
                ),
            )
            controller.onSendResult(result is ApiResult.Success)
            sending = false
        }
    }

    private fun stopTracking() {
        fusedClient.removeLocationUpdates(callback)
        routeId = NO_ROUTE
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        fusedClient.removeLocationUpdates(callback)
        controller.onServiceStopped()
        super.onDestroy()
    }

    private fun buildNotification(routeName: String): Notification =
        NotificationCompat.Builder(this, SystemNotifier.CHANNEL_TRACKING)
            .setSmallIcon(R.drawable.ic_stat_bus)
            .setContentTitle("Rota em andamento")
            .setContentText(if (routeName.isBlank()) "Enviando sua localização" else "$routeName · enviando sua localização")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(SystemNotifier.openAppIntent(this))
            .build()

    companion object {
        private const val ACTION_START = "com.rotacerta.mobile.tracking.START"
        private const val ACTION_STOP = "com.rotacerta.mobile.tracking.STOP"
        private const val EXTRA_ROUTE_ID = "route_id"
        private const val EXTRA_ROUTE_NAME = "route_name"
        private const val NO_ROUTE = -1L
        private const val NOTIFICATION_ID = 1001

        private const val UPDATE_INTERVAL_MS = 5_000L
        private const val MIN_UPDATE_INTERVAL_MS = 3_000L
        private const val MIN_DISTANCE_METERS = 10f

        fun startIntent(context: Context, routeId: Long, routeName: String) =
            Intent(context, LocationTrackingService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_ROUTE_ID, routeId)
                .putExtra(EXTRA_ROUTE_NAME, routeName)

        fun stopIntent(context: Context) =
            Intent(context, LocationTrackingService::class.java).setAction(ACTION_STOP)
    }
}
