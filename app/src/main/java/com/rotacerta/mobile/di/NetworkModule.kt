package com.rotacerta.mobile.di

import com.rotacerta.mobile.BuildConfig
import com.rotacerta.mobile.core.network.AuthInterceptor
import com.rotacerta.mobile.core.network.TokenAuthenticator
import com.rotacerta.mobile.data.api.AddressApi
import com.rotacerta.mobile.data.api.AuthApi
import com.rotacerta.mobile.data.api.CollaboratorApi
import com.rotacerta.mobile.data.api.DriverApi
import com.rotacerta.mobile.data.api.NotificationApi
import com.rotacerta.mobile.data.api.PushApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        // O DRF serializa DecimalField (latitude/longitude) como string ("-3.100000");
        // isLenient decodifica esses números entre aspas direto em Double.
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .authenticator(tokenAuthenticator)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
                // O WebSocket leva o JWT na query string (?token=); não pode aparecer no logcat.
                redactQueryParams("token")
                redactHeader("Authorization")
            },
        )
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideDriverApi(retrofit: Retrofit): DriverApi = retrofit.create(DriverApi::class.java)

    @Provides
    @Singleton
    fun provideCollaboratorApi(retrofit: Retrofit): CollaboratorApi = retrofit.create(CollaboratorApi::class.java)

    @Provides
    @Singleton
    fun provideNotificationApi(retrofit: Retrofit): NotificationApi = retrofit.create(NotificationApi::class.java)

    @Provides
    @Singleton
    fun provideAddressApi(retrofit: Retrofit): AddressApi = retrofit.create(AddressApi::class.java)

    @Provides
    @Singleton
    fun providePushApi(retrofit: Retrofit): PushApi = retrofit.create(PushApi::class.java)
}
