package com.rotacerta.mobile.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rotacerta.mobile.MainActivity
import com.rotacerta.mobile.R
import com.rotacerta.mobile.data.dto.NotificationDto
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Notificações do sistema Android (avisos recebidos com o app em segundo plano). */
@Singleton
class SystemNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun createChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_NOTICES, "Avisos da rota", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Saída do ônibus, atrasos, chegada ao ponto e confirmações de embarque."
                },
                NotificationChannel(CHANNEL_TRACKING, "Rota em andamento", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Mostrada enquanto o app envia a localização do ônibus."
                },
            ),
        )
    }

    fun canPost(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun show(notification: NotificationDto) {
        if (!canPost()) return
        val built = NotificationCompat.Builder(context, CHANNEL_NOTICES)
            .setSmallIcon(R.drawable.ic_stat_bus)
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notification.id.toInt(), built)
        } catch (_: SecurityException) {
            // permissão revogada entre a checagem e o envio
        }
    }

    companion object {
        const val CHANNEL_NOTICES = "notices"
        const val CHANNEL_TRACKING = "tracking"

        fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
