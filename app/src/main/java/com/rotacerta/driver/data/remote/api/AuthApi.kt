package com.rotacerta.driver.data.remote.api

import com.rotacerta.driver.data.remote.dto.AuthSessionResponse
import com.rotacerta.driver.data.remote.dto.ChangePasswordRequest
import com.rotacerta.driver.data.remote.dto.LoginRequest
import com.rotacerta.driver.data.remote.dto.PasswordResetCompletePayload
import com.rotacerta.driver.data.remote.dto.PasswordResetRequestPayload
import com.rotacerta.driver.data.remote.dto.PasswordResetRequestResponse
import com.rotacerta.driver.data.remote.dto.RefreshRequest
import com.rotacerta.driver.data.remote.dto.RefreshResponse
import com.rotacerta.driver.data.remote.dto.StatusResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/login/")
    suspend fun login(@Body body: LoginRequest): Response<AuthSessionResponse>

    @POST("auth/refresh/")
    suspend fun refresh(@Body body: RefreshRequest): Response<RefreshResponse>

    @POST("auth/change-password/")
    suspend fun changePassword(@Body body: ChangePasswordRequest): Response<AuthSessionResponse>

    @POST("auth/password-reset/request/")
    suspend fun requestPasswordReset(
        @Body body: PasswordResetRequestPayload,
    ): Response<PasswordResetRequestResponse>

    @POST("auth/password-reset/reset/")
    suspend fun completePasswordReset(
        @Body body: PasswordResetCompletePayload,
    ): Response<StatusResponse>
}
