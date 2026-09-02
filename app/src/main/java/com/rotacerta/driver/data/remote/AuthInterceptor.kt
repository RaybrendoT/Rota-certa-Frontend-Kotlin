package com.rotacerta.driver.data.remote

import com.rotacerta.driver.data.local.SessionStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Anexa "Authorization: Bearer <access_token>" em toda chamada, exceto nos
 * endpoints públicos de autenticação (login, refresh, reset de senha).
 */
class AuthInterceptor @Inject constructor(
    private val sessionStore: SessionStore,
) : Interceptor {

    private val publicPaths = listOf(
        "auth/login/",
        "auth/refresh/",
        "auth/password-reset/",
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val isPublic = publicPaths.any { path.contains(it) }

        if (isPublic) {
            return chain.proceed(request)
        }

        val token = runBlocking { sessionStore.getAccessToken() }
        val authorizedRequest = if (!token.isNullOrBlank()) {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }
        return chain.proceed(authorizedRequest)
    }
}
