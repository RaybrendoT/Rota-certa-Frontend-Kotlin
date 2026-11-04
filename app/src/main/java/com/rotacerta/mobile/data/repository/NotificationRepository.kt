package com.rotacerta.mobile.data.repository

import com.rotacerta.mobile.core.network.ApiResult
import com.rotacerta.mobile.core.network.apiCall
import com.rotacerta.mobile.core.notification.SystemNotifier
import com.rotacerta.mobile.core.realtime.RealtimeClient
import com.rotacerta.mobile.core.realtime.RealtimeEvent
import com.rotacerta.mobile.core.session.SessionStore
import com.rotacerta.mobile.core.util.AppForegroundTracker
import com.rotacerta.mobile.data.api.NotificationApi
import com.rotacerta.mobile.data.dto.NotificationDto
import com.rotacerta.mobile.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class NotificationsState(
    val items: List<NotificationDto> = emptyList(),
    val unreadCount: Int = 0,
    val loaded: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val nextPage: Int? = null,
)

/** Aba Avisos: histórico pela API + novos avisos ao vivo pelo WebSocket. */
@Singleton
class NotificationRepository @Inject constructor(
    private val api: NotificationApi,
    private val systemNotifier: SystemNotifier,
    private val foregroundTracker: AppForegroundTracker,
    realtimeClient: RealtimeClient,
    sessionStore: SessionStore,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(NotificationsState())
    val state: StateFlow<NotificationsState> = _state.asStateFlow()

    /** Avisos que chegaram com o app aberto (a tela mostra um snackbar). */
    private val _foregroundAlerts = MutableSharedFlow<NotificationDto>(extraBufferCapacity = 8)
    val foregroundAlerts: SharedFlow<NotificationDto> = _foregroundAlerts.asSharedFlow()

    init {
        scope.launch {
            realtimeClient.events.collect { event ->
                when (event) {
                    is RealtimeEvent.NotificationReceived -> onReceived(event.notification)
                    // Reconectou: recarrega para pegar o que chegou sem conexão.
                    is RealtimeEvent.Connected -> refresh()
                    else -> Unit
                }
            }
        }
        scope.launch { sessionStore.session.collect { if (it == null) _state.value = NotificationsState() } }
    }

    suspend fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        val page = apiCall("Não foi possível carregar os avisos.") { api.list(page = 1) }
        val unread = apiCall("") { api.list(page = 1, unread = true) }
        when (page) {
            is ApiResult.Success -> _state.update {
                it.copy(
                    items = page.data.results,
                    unreadCount = (unread as? ApiResult.Success)?.data?.count ?: page.data.results.count { n -> !n.read },
                    loaded = true,
                    loading = false,
                    nextPage = if (page.data.next != null) 2 else null,
                )
            }
            is ApiResult.Failure -> _state.update { it.copy(loading = false, error = page.message) }
        }
    }

    suspend fun loadMore() {
        val next = _state.value.nextPage ?: return
        if (_state.value.loading) return
        _state.update { it.copy(loading = true) }
        when (val page = apiCall("Não foi possível carregar mais avisos.") { api.list(page = next) }) {
            is ApiResult.Success -> _state.update { current ->
                val known = current.items.map { it.id }.toSet()
                current.copy(
                    items = current.items + page.data.results.filterNot { it.id in known },
                    loading = false,
                    nextPage = if (page.data.next != null) next + 1 else null,
                )
            }
            is ApiResult.Failure -> _state.update { it.copy(loading = false, error = page.message) }
        }
    }

    suspend fun markRead(id: Long) {
        val target = _state.value.items.firstOrNull { it.id == id } ?: return
        if (target.read) return
        _state.update { current ->
            current.copy(
                items = current.items.map { if (it.id == id) it.copy(read = true) else it },
                unreadCount = (current.unreadCount - 1).coerceAtLeast(0),
            )
        }
        apiCall("") { api.markRead(id) }
    }

    suspend fun markAllRead() {
        _state.update { current -> current.copy(items = current.items.map { it.copy(read = true) }, unreadCount = 0) }
        apiCall("") { api.markAllRead() }
    }

    private fun onReceived(notification: NotificationDto) {
        var isNew = false
        _state.update { current ->
            if (current.items.any { it.id == notification.id }) {
                current
            } else {
                isNew = true
                current.copy(items = listOf(notification) + current.items, unreadCount = current.unreadCount + 1)
            }
        }
        if (!isNew) return
        if (foregroundTracker.isForeground.value) {
            _foregroundAlerts.tryEmit(notification)
        } else {
            systemNotifier.show(notification)
        }
    }
}
