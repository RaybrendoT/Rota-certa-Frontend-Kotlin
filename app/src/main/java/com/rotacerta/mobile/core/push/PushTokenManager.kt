package com.rotacerta.mobile.core.push

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.rotacerta.mobile.core.network.apiCall
import com.rotacerta.mobile.core.session.SessionStore
import com.rotacerta.mobile.data.api.PushApi
import com.rotacerta.mobile.data.dto.PushDeviceRequest
import com.rotacerta.mobile.data.dto.PushTokenRequest
import com.rotacerta.mobile.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registra o aparelho no backend (POST /me/devices/) para receber notificações push:
 * a cada login e quando o Firebase troca o token. No logout, remove antes de apagar o JWT.
 * Sem o google-services.json no build, o Firebase não existe e tudo aqui vira no-op.
 */
@Singleton
class PushTokenManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val api: PushApi,
    private val sessionStore: SessionStore,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    val isAvailable: Boolean
        get() = FirebaseApp.getApps(context).isNotEmpty()

    fun start() {
        if (!isAvailable) {
            Log.i(TAG, "Firebase não configurado (sem google-services.json): push desligado.")
            return
        }
        scope.launch {
            sessionStore.session.map { it?.personId }.distinctUntilChanged().collect { personId ->
                if (personId != null) register()
            }
        }
    }

    fun onNewToken(token: String) {
        scope.launch { register(token) }
    }

    suspend fun register(token: String? = null) {
        if (!isAvailable || sessionStore.session.first() == null) return
        val value = token ?: currentToken() ?: return
        apiCall("") { api.register(PushDeviceRequest(value)) }
    }

    suspend fun unregister() {
        if (!isAvailable || sessionStore.session.first() == null) return
        val value = currentToken() ?: return
        apiCall("") { api.unregister(PushTokenRequest(value)) }
    }

    private suspend fun currentToken(): String? = try {
        FirebaseMessaging.getInstance().token.await()
    } catch (e: Exception) {
        Log.w(TAG, "Não foi possível obter o token do Firebase.", e)
        null
    }

    private companion object {
        const val TAG = "PushTokenManager"
    }
}
