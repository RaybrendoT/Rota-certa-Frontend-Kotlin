package com.rotacerta.driver.data.remote

import com.rotacerta.driver.BuildConfig
import com.rotacerta.driver.data.local.SessionStore
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Quando qualquer chamada recebe 401, tenta renovar o access_token usando o
 * refresh_token guardado (POST /auth/refresh/) e repete a requisição original.
 * Se a renovação falhar (refresh expirado/inválido), limpa a sessão e desiste
 * — a UI observa SessionStore e volta para a tela de login.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val sessionStore: SessionStore,
) : Authenticator {

    // Cliente "cru", sem AuthInterceptor/TokenAuthenticator, só para o refresh.
    private val plainClient = OkHttpClient.Builder().build()
    private val json = Json { ignoreUnknownKeys = true }

    override fun authenticate(route: Route?, response: Response): Request? {
        // Evita loop infinito: só tenta renovar uma vez por requisição.
        if (responseCount(response) >= 2) return null

        val refreshToken = runBlocking { sessionStore.getRefreshToken() } ?: return null

        val newAccessToken = runBlocking { refreshAccessToken(refreshToken) } ?: run {
            runBlocking { sessionStore.clear() }
            return null
        }

        runBlocking { sessionStore.updateAccessToken(newAccessToken) }

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccessToken")
            .build()
    }

    private suspend fun refreshAccessToken(refreshToken: String): String? {
        val body = """{"refresh":"$refreshToken"}"""
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(BuildConfig.API_BASE_URL + "auth/refresh/")
            .post(body)
            .build()

        return try {
            plainClient.newCall(request).execute().use { httpResponse ->
                if (!httpResponse.isSuccessful) return null
                val text = httpResponse.body?.string() ?: return null
                json.parseToJsonElement(text).jsonObject["access_token"]
                    ?.jsonPrimitive?.content
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
