package com.rotacerta.driver.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rotacerta.driver.data.remote.dto.AuthSessionResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "rota_certa_session")

data class DriverSession(
    val personId: Long,
    val personName: String,
    val companyName: String,
    val mustChangePassword: Boolean,
)

/**
 * Guarda tokens JWT e dados básicos da pessoa logada.
 * Usado pelo interceptor de autenticação e pelas telas para saber se há sessão ativa.
 */
@Singleton
class SessionStore @Inject constructor(
    private val context: Context,
) {
    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val PERSON_ID = longPreferencesKey("person_id")
        val PERSON_NAME = stringPreferencesKey("person_name")
        val COMPANY_NAME = stringPreferencesKey("company_name")
        val MUST_CHANGE_PASSWORD = booleanPreferencesKey("must_change_password")
    }

    private val prefsFlow = context.dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val sessionFlow: Flow<DriverSession?> = prefsFlow.map { prefs ->
        val personId = prefs[Keys.PERSON_ID] ?: return@map null
        val accessToken = prefs[Keys.ACCESS_TOKEN]
        if (accessToken.isNullOrBlank()) return@map null
        DriverSession(
            personId = personId,
            personName = prefs[Keys.PERSON_NAME].orEmpty(),
            companyName = prefs[Keys.COMPANY_NAME].orEmpty(),
            mustChangePassword = prefs[Keys.MUST_CHANGE_PASSWORD] ?: false,
        )
    }

    suspend fun getAccessToken(): String? = prefsFlow.map { it[Keys.ACCESS_TOKEN] }.first()

    suspend fun getRefreshToken(): String? = prefsFlow.map { it[Keys.REFRESH_TOKEN] }.first()

    suspend fun saveSession(response: AuthSessionResponse) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = response.accessToken
            prefs[Keys.REFRESH_TOKEN] = response.refreshToken
            prefs[Keys.PERSON_ID] = response.person.id
            prefs[Keys.PERSON_NAME] = response.person.name
            prefs[Keys.COMPANY_NAME] = response.person.companyName
            prefs[Keys.MUST_CHANGE_PASSWORD] = response.mustChangePassword
        }
    }

    suspend fun updateAccessToken(newAccessToken: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = newAccessToken
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
