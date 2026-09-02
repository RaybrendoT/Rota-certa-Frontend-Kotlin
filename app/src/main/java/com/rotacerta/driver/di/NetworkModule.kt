package com.rotacerta.driver.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.rotacerta.driver.BuildConfig
import com.rotacerta.driver.data.remote.AuthInterceptor
import com.rotacerta.driver.data.remote.TokenAuthenticator
import com.rotacerta.driver.data.remote.api.AuthApi
import com.rotacerta.driver.data.remote.api.RouteApi
import com.rotacerta.driver.data.remote.api.RouteEventApi
import com.rotacerta.driver.data.remote.api.RoutePassengerApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
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
        // O Django REST Framework serializa DecimalField (latitude/longitude) como
        // string (ex: "-23.550520") por padrão (COERCE_DECIMAL_TO_STRING=True).
        // isLenient permite decodificar esses números "entre aspas" diretamente
        // em campos Double, sem precisar de um serializer customizado por DTO.
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .authenticator(tokenAuthenticator)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideRouteApi(retrofit: Retrofit): RouteApi = retrofit.create(RouteApi::class.java)

    @Provides
    @Singleton
    fun provideRouteEventApi(retrofit: Retrofit): RouteEventApi =
        retrofit.create(RouteEventApi::class.java)

    @Provides
    @Singleton
    fun provideRoutePassengerApi(retrofit: Retrofit): RoutePassengerApi =
        retrofit.create(RoutePassengerApi::class.java)
}
