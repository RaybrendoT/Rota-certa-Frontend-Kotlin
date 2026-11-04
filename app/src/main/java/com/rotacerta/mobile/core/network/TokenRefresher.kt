package com.rotacerta.mobile.core.network

import com.rotacerta.mobile.BuildConfig
import com.rotacerta.mobile.core.session.SessionStore
import com.rotacerta.mobile.data.dto.RefreshRequest
import com.rotacerta.mobile.data.dto.RefreshResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Renova o access token com o refresh token (POST /auth/refresh/).
 * Compartilhado pelo HTTP (TokenAuthenticator) e pelo WebSocket (fechamento 4001);
 * o Mutex evita várias renovações simultâneas.
 */
@Singleton
class TokenRefresher @Inject constructor(
    private val sessionStore: SessionStore,
    private val json: Json,
) {
    // Cliente sem interceptor/authenticator, só para o refresh.
    private val client = OkHttpClient()
    private val mutex = Mutex()

    /**
     * @param failedToken token que acabou de ser recusado; se outro chamador já renovou,
     * devolve o token novo sem chamar o backend de novo.
     * @return o novo access token, ou null se a sessão expirou (e então ela é encerrada).
     */
    suspend fun refresh(failedToken: String?): String? = mutex.withLock {
        val current = sessionStore.accessToken()
        if (current != null && current != failedToken) return@withLock current

        val refreshToken = sessionStore.refreshToken() ?: return@withLock null
        val newToken = withContext(Dispatchers.IO) { requestNewToken(refreshToken) }
        if (newToken == null) {
            sessionStore.clear() // a UI volta ao login sozinha
        } else {
            sessionStore.updateAccessToken(newToken)
        }
        newToken
    }

    private fun requestNewToken(refreshToken: String): String? {
        val body = json.encodeToString(RefreshRequest.serializer(), RefreshRequest(refreshToken))
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(BuildConfig.API_BASE_URL + "auth/refresh/").post(body).build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                json.decodeFromString(RefreshResponse.serializer(), response.body.string()).accessToken
            }
        }.getOrNull()
    }
}
