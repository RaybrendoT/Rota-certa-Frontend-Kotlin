package com.rotacerta.mobile.data.repository

import com.rotacerta.mobile.core.network.ApiResult
import com.rotacerta.mobile.core.network.apiCall
import com.rotacerta.mobile.core.network.map
import com.rotacerta.mobile.core.session.SessionStore
import com.rotacerta.mobile.data.api.AuthApi
import com.rotacerta.mobile.data.dto.ChangePasswordRequest
import com.rotacerta.mobile.data.dto.LoginRequest
import com.rotacerta.mobile.data.dto.PasswordResetCompletePayload
import com.rotacerta.mobile.data.dto.PasswordResetIdentifier
import com.rotacerta.mobile.data.dto.PasswordResetVerifyPayload
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val sessionStore: SessionStore,
) {
    /** Login de motorista ou colaborador; a sessão salva decide para qual área o app vai. */
    suspend fun login(email: String, password: String): ApiResult<Unit> =
        apiCall("E-mail ou senha inválidos.") { authApi.login(LoginRequest(email.trim().lowercase(), password)) }
            .also { if (it is ApiResult.Success) sessionStore.save(it.data) }
            .map { }

    suspend fun changePassword(currentPassword: String, newPassword: String): ApiResult<Unit> =
        apiCall("Não foi possível trocar a senha.") {
            authApi.changePassword(ChangePasswordRequest(currentPassword, newPassword))
        }
            .also { if (it is ApiResult.Success) sessionStore.save(it.data) }
            .map { }

    /** @return o código de verificação quando o backend está em DEBUG (para testes), senão null. */
    suspend fun requestPasswordReset(identifier: PasswordResetIdentifier): ApiResult<String?> =
        apiCall("Não foi possível solicitar o código.") { authApi.requestPasswordReset(identifier) }
            .map { it.verificationCode }

    suspend fun verifyPasswordReset(identifier: PasswordResetIdentifier, code: String): ApiResult<Boolean> =
        apiCall("Código inválido ou expirado.") {
            authApi.verifyPasswordReset(PasswordResetVerifyPayload(identifier.email, identifier.phone, code.trim()))
        }.map { it.valid }

    suspend fun completePasswordReset(
        identifier: PasswordResetIdentifier,
        code: String,
        newPassword: String,
    ): ApiResult<Unit> = apiCall("Não foi possível redefinir a senha.") {
        authApi.completePasswordReset(
            PasswordResetCompletePayload(identifier.email, identifier.phone, code.trim(), newPassword),
        )
    }.map { }

    /** Encerra a sessão local; WebSocket, rastreamento e caches reagem à sessão nula. */
    suspend fun logout() = sessionStore.clear()
}
