package com.rotacerta.driver.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class RefreshRequest(
    val refresh: String,
)

@Serializable
data class RefreshResponse(
    @SerialName("access_token") val accessToken: String,
)

@Serializable
data class ChangePasswordRequest(
    @SerialName("current_password") val currentPassword: String,
    @SerialName("new_password") val newPassword: String,
)

/**
 * Payload comum devolvido pelo login e pela troca de senha:
 * novo par de tokens + flag de senha obrigatória + dados básicos da pessoa logada.
 */
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
    val type: String, // "DRIVER" | "EMPLOYEE"
    val email: String? = null,
    @SerialName("company_id") val companyId: Long,
    @SerialName("company_name") val companyName: String,
)

@Serializable
data class PasswordResetRequestPayload(
    val email: String? = null,
    val phone: String? = null,
)

@Serializable
data class PasswordResetRequestResponse(
    val status: String,
    val verificationCode: String? = null, // só vem preenchido se o backend estiver em DEBUG
)

@Serializable
data class PasswordResetCompletePayload(
    val email: String? = null,
    val phone: String? = null,
    val code: String,
    @SerialName("new_password") val newPassword: String,
)

@Serializable
data class StatusResponse(
    val status: String? = null,
    val detail: String? = null,
)
