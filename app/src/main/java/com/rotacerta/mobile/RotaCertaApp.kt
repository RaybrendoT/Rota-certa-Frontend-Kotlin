package com.rotacerta.mobile

import android.app.Application
import com.rotacerta.mobile.core.notification.SystemNotifier
import com.rotacerta.mobile.core.push.PushTokenManager
import com.rotacerta.mobile.core.realtime.RealtimeClient
import com.rotacerta.mobile.core.util.AppForegroundTracker
import com.rotacerta.mobile.data.repository.NotificationRepository
import com.rotacerta.mobile.feature.driver.tracking.TrackingController
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class RotaCertaApp : Application() {

    @Inject lateinit var systemNotifier: SystemNotifier
    @Inject lateinit var foregroundTracker: AppForegroundTracker
    @Inject lateinit var realtimeClient: RealtimeClient

    // Injetados só para existirem desde o início: escutam o WebSocket e a sessão.
    @Inject lateinit var notificationRepository: NotificationRepository
    @Inject lateinit var trackingController: TrackingController
    @Inject lateinit var pushTokenManager: PushTokenManager

    override fun onCreate() {
        super.onCreate()
        systemNotifier.createChannels()
        foregroundTracker.register()
        realtimeClient.start()
        pushTokenManager.start()
    }
}
