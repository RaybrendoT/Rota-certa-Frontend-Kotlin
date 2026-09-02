package com.rotacerta.driver.data.repository

import com.rotacerta.driver.data.local.DriverSession
import com.rotacerta.driver.data.local.SessionStore
import com.rotacerta.driver.data.remote.api.AuthApi
import com.rotacerta.driver.data.remote.dto.ChangePasswordRequest
import com.rotacerta.driver.data.remote.dto.LoginRequest
import com.rotacerta.driver.data.remote.dto.PasswordResetCompletePayload
import com.rotacerta.driver.data.remote.dto.PasswordResetRequestPayload
import com.rotacerta.driver.util.extractErrorMessage
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AuthResult {
    data class Success(val mustChangePassword: Boolean) : AuthResult
    data class Failure(val message: String) : AuthResult
}

sealed interface SimpleResult {
    data object Success : SimpleResult
    data class Failure(val message: String) : SimpleResult
}

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val sessionStore: SessionStore,
) {
    val session: Flow<DriverSession?> = sessionStore.sessionFlow

    suspend fun login(email: String, password: String): AuthResult {
        return try {
            val response = authApi.login(LoginRequest(email.trim(), password))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                if (body.person.type != "DRIVER") {
                    AuthResult.Failure(
                        "Este app é exclusivo para motoristas. A conta informada não é de motorista.",
                    )
                } else {
                    sessionStore.saveSession(body)
                    AuthResult.Success(body.mustChangePassword)
                }
            } else {
                AuthResult.Failure(response.extractErrorMessage("E-mail ou senha inválidos."))
            }
        } catch (e: Exception) {
            AuthResult.Failure("Não foi possível conectar ao servidor. Verifique sua internet.")
        }
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): AuthResult {
        return try {
            val response = authApi.changePassword(
                ChangePasswordRequest(currentPassword, newPassword),
            )
            val body = response.body()
            if (response.isSuccessful && body != null) {
                sessionStore.saveSession(body)
                AuthResult.Success(body.mustChangePassword)
            } else {
                AuthResult.Failure(response.extractErrorMessage("Não foi possível trocar a senha."))
            }
        } catch (e: Exception) {
            AuthResult.Failure("Não foi possível conectar ao servidor. Verifique sua internet.")
        }
    }

    suspend fun requestPasswordReset(email: String): SimpleResult {
        return try {
            val response = authApi.requestPasswordReset(PasswordResetRequestPayload(email = email.trim()))
            if (response.isSuccessful) {
                SimpleResult.Success
            } else {
                SimpleResult.Failure(response.extractErrorMessage("Não foi possível solicitar o código."))
            }
        } catch (e: Exception) {
            SimpleResult.Failure("Não foi possível conectar ao servidor. Verifique sua internet.")
        }
    }

    suspend fun completePasswordReset(email: String, code: String, newPassword: String): SimpleResult {
        return try {
            val response = authApi.completePasswordReset(
                PasswordResetCompletePayload(email = email.trim(), code = code.trim(), newPassword = newPassword),
            )
            if (response.isSuccessful) {
                SimpleResult.Success
            } else {
                SimpleResult.Failure(response.extractErrorMessage("Código inválido ou expirado."))
            }
        } catch (e: Exception) {
            SimpleResult.Failure("Não foi possível conectar ao servidor. Verifique sua internet.")
        }
    }

    suspend fun logout() {
        sessionStore.clear()
    }
}
