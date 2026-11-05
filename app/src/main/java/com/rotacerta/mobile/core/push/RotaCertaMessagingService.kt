package com.rotacerta.mobile.core.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.rotacerta.mobile.core.notification.SystemNotifier
import com.rotacerta.mobile.core.util.AppForegroundTracker
import com.rotacerta.mobile.data.dto.NotificationDto
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Recebe as notificações push do backend. Elas chegam só com "data" (sem o bloco
 * "notification"), então este método roda também com o app fechado ou em segundo plano.
 *
 * Usa o mesmo id da notificação do backend que o aviso do WebSocket: se os dois chegarem,
 * o Android substitui um pelo outro em vez de mostrar duas vezes. Com o app aberto, quem
 * mostra é o WebSocket (snackbar), então o push é ignorado.
 */
@AndroidEntryPoint
class RotaCertaMessagingService : FirebaseMessagingService() {

    @Inject lateinit var pushTokenManager: PushTokenManager
    @Inject lateinit var systemNotifier: SystemNotifier
    @Inject lateinit var foregroundTracker: AppForegroundTracker

    override fun onNewToken(token: String) {
        pushTokenManager.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val id = data["notification_id"]?.toLongOrNull() ?: return
        if (foregroundTracker.isForeground.value) return
        systemNotifier.show(
            NotificationDto(
                id = id,
                kind = data["kind"].orEmpty(),
                title = data["title"] ?: message.notification?.title ?: "Rota Certa",
                message = data["message"] ?: message.notification?.body.orEmpty(),
                routeId = data["route_id"]?.toLongOrNull(),
                createdAt = "",
            ),
        )
    }
}
