package com.rotacerta.mobile.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RefreshRequest(val refresh: String)

@Serializable
data class RefreshResponse(@SerialName("access_token") val accessToken: String)

@Serializable
data class ChangePasswordRequest(
    @SerialName("current_password") val currentPassword: String,
    @SerialName("new_password") val newPassword: String,
)

/** Resposta do login e da troca de senha: tokens + dados da pessoa logada. */
@Serializable
data class AuthSessionResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("must_change_password") val mustChangePassword: Boolean,
    val person: PersonBriefDto,
)

@Serializable
data class PersonBriefDto(
    val id: Long,
    val name: String,
    /** "DRIVER" ou "EMPLOYEE" — decide qual área do app a pessoa usa. */
    val type: String,
    val email: String? = null,
    /** Perfil da conta: DRIVER, EMPLOYEE, MANAGER ou GERENTE. */
    val role: String? = null,
    @SerialName("company_id") val companyId: Long,
    @SerialName("company_name") val companyName: String,
)

/** Identificador da recuperação de senha: exatamente um entre e-mail e telefone. */
@Serializable
data class PasswordResetIdentifier(
    val email: String? = null,
    val phone: String? = null,
)

@Serializable
data class PasswordResetRequestResponse(
    val status: String? = null,
    /** Só vem preenchido com o backend em DEBUG (PASSWORD_RESET_RETURN_CODE_IN_RESPONSE). */
    val verificationCode: String? = null,
)

@Serializable
data class PasswordResetVerifyPayload(
    val email: String? = null,
    val phone: String? = null,
    val code: String,
)

@Serializable
data class PasswordResetVerifyResponse(val valid: Boolean = false)

@Serializable
data class PasswordResetCompletePayload(
    val email: String? = null,
    val phone: String? = null,
    val code: String,
    @SerialName("new_password") val newPassword: String,
)

@Serializable
data class StatusResponse(val status: String? = null, val detail: String? = null)
