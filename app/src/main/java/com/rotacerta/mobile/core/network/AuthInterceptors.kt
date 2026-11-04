package com.rotacerta.mobile.core.network

import com.rotacerta.mobile.core.session.SessionStore
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

private val PUBLIC_PATHS = listOf("auth/login/", "auth/refresh/", "auth/password-reset/")

/** Anexa "Authorization: Bearer <access_token>", exceto nos endpoints públicos de auth. */
@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionStore: SessionStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (PUBLIC_PATHS.any { request.url.encodedPath.contains(it) }) return chain.proceed(request)

        val token = runBlocking { sessionStore.accessToken() }
        return chain.proceed(
            if (token.isNullOrBlank()) request else request.newBuilder().header("Authorization", "Bearer $token").build(),
        )
    }
}

/** Em um 401, renova o token uma vez e repete a requisição; sem refresh válido, encerra a sessão. */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenRefresher: TokenRefresher,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponse != null) return null // já tentou renovar nesta requisição
        if (PUBLIC_PATHS.any { response.request.url.encodedPath.contains(it) }) return null

        val failedToken = response.request.header("Authorization")?.removePrefix("Bearer ")
        val newToken = runBlocking { tokenRefresher.refresh(failedToken) } ?: return null
        return response.request.newBuilder().header("Authorization", "Bearer $newToken").build()
    }
}
