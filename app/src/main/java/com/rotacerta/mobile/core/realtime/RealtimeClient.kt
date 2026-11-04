package com.rotacerta.mobile.core.realtime

import com.rotacerta.mobile.BuildConfig
import com.rotacerta.mobile.core.network.TokenRefresher
import com.rotacerta.mobile.core.session.SessionStore
import com.rotacerta.mobile.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.math.min

enum class ConnectionState { Disconnected, Connecting, Connected }

/** Código de fechamento do backend para token ausente/inválido/expirado. */
const val CLOSE_UNAUTHORIZED = 4001

/**
 * Conexão única com /ws/stream/ enquanto houver sessão pronta.
 *
 * - Conecta ao logar e desconecta ao sair (observa a sessão).
 * - Reconecta com backoff exponencial (1 s → 30 s) quando a conexão cai.
 * - Fechamento 4001: renova o token e reconecta; sem refresh válido a sessão é encerrada.
 */
@Singleton
class RealtimeClient @Inject constructor(
    baseClient: OkHttpClient,
    private val sessionStore: SessionStore,
    private val tokenRefresher: TokenRefresher,
    private val json: Json,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    private val client = baseClient.newBuilder()
        .pingInterval(25, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val _state = MutableStateFlow(ConnectionState.Disconnected)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<RealtimeEvent> = _events.asSharedFlow()

    private var job: Job? = null

    fun start() {
        if (job != null) return
        job = scope.launch {
            sessionStore.session
                .map { session -> session?.takeUnless { it.mustChangePassword }?.personId }
                .distinctUntilChanged()
                .collectLatest { personId ->
                    if (personId == null) {
                        _state.value = ConnectionState.Disconnected
                    } else {
                        connectionLoop()
                    }
                }
        }
    }

    private suspend fun connectionLoop() {
        var attempt = 0
        while (true) {
            val token = sessionStore.accessToken() ?: return
            _state.value = ConnectionState.Connecting
            val outcome = connectUntilClosed(token)
            _state.value = ConnectionState.Disconnected

            if (outcome.unauthorized) {
                tokenRefresher.refresh(failedToken = token) ?: return // sessão expirou: volta ao login
                attempt = 0
                continue
            }
            if (outcome.wasOpen) attempt = 0
            delay(backoffMillis(attempt++))
        }
    }

    private class Outcome(val wasOpen: Boolean, val unauthorized: Boolean)

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun connectUntilClosed(token: String): Outcome = suspendCancellableCoroutine { cont ->
        var wasOpen = false
        fun finish(unauthorized: Boolean) {
            if (cont.isActive) cont.resume(Outcome(wasOpen, unauthorized))
        }

        val request = Request.Builder().url("${BuildConfig.WS_URL}?token=$token").build()
        val socket = client.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    wasOpen = true
                    _state.value = ConnectionState.Connected
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    RealtimeParser.parse(json, text)?.let { _events.tryEmit(it) }
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(1000, null)
                    finish(unauthorized = code == CLOSE_UNAUTHORIZED)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    finish(unauthorized = false)
                }
            },
        )
        cont.invokeOnCancellation { socket.close(1000, "logout") }
    }

    companion object {
        fun backoffMillis(attempt: Int): Long = min(30_000L, 1_000L shl min(attempt, 5))
    }
}
