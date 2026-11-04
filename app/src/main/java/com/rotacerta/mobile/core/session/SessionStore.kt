package com.rotacerta.mobile.core.session

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rotacerta.mobile.data.dto.AuthSessionResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "rota_certa_session")

/** Área do app que a pessoa usa, decidida pelo tipo da pessoa no backend. */
enum class UserRole {
    DRIVER,
    COLLABORATOR;

    companion object {
        fun fromPersonType(type: String?): UserRole = if (type == "DRIVER") DRIVER else COLLABORATOR
    }
}

data class UserSession(
    val personId: Long,
    val name: String,
    val companyName: String,
    val role: UserRole,
    val mustChangePassword: Boolean,
)

/** Tokens JWT e dados básicos da pessoa logada (DataStore). */
@Singleton
class SessionStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val PERSON_ID = longPreferencesKey("person_id")
        val PERSON_NAME = stringPreferencesKey("person_name")
        val PERSON_TYPE = stringPreferencesKey("person_type")
        val COMPANY_NAME = stringPreferencesKey("company_name")
        val MUST_CHANGE_PASSWORD = booleanPreferencesKey("must_change_password")
    }

    private val prefs = context.dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val session: Flow<UserSession?> = prefs.map { p ->
        val personId = p[Keys.PERSON_ID] ?: return@map null
        if (p[Keys.ACCESS_TOKEN].isNullOrBlank()) return@map null
        UserSession(
            personId = personId,
            name = p[Keys.PERSON_NAME].orEmpty(),
            companyName = p[Keys.COMPANY_NAME].orEmpty(),
            role = UserRole.fromPersonType(p[Keys.PERSON_TYPE]),
            mustChangePassword = p[Keys.MUST_CHANGE_PASSWORD] ?: false,
        )
    }.distinctUntilChanged()

    suspend fun current(): UserSession? = session.first()

    suspend fun accessToken(): String? = prefs.map { it[Keys.ACCESS_TOKEN] }.first()

    suspend fun refreshToken(): String? = prefs.map { it[Keys.REFRESH_TOKEN] }.first()

    suspend fun save(response: AuthSessionResponse) {
        context.dataStore.edit { p ->
            p[Keys.ACCESS_TOKEN] = response.accessToken
            p[Keys.REFRESH_TOKEN] = response.refreshToken
            p[Keys.PERSON_ID] = response.person.id
            p[Keys.PERSON_NAME] = response.person.name
            p[Keys.PERSON_TYPE] = response.person.type
            p[Keys.COMPANY_NAME] = response.person.companyName
            p[Keys.MUST_CHANGE_PASSWORD] = response.mustChangePassword
        }
    }

    suspend fun updateAccessToken(token: String) {
        context.dataStore.edit { it[Keys.ACCESS_TOKEN] = token }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
