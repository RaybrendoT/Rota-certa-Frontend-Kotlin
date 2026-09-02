package com.rotacerta.driver.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.Response

private val errorJson = Json { ignoreUnknownKeys = true }

/**
 * O Django REST Framework devolve erros em formatos como:
 *   {"detail": "Credenciais inválidas."}
 *   {"email": ["Este campo é obrigatório."], "password": [...]}
 * Esta função tenta extrair uma mensagem legível para mostrar ao motorista.
 */
fun Response<*>.extractErrorMessage(fallback: String = "Ocorreu um erro. Tente novamente."): String {
    val body = errorBody()?.string()
    if (body.isNullOrBlank()) return fallback

    return try {
        when (val element = errorJson.parseToJsonElement(body)) {
            is JsonObject -> {
                (element["detail"] as? JsonPrimitive)?.content
                    ?: element.entries.firstOrNull()?.let { (field, value) ->
                        val message = when (value) {
                            is JsonArray -> value.firstOrNull()?.jsonPrimitive?.content
                            is JsonPrimitive -> value.content
                            else -> null
                        }
                        message?.let { "$field: $it" }
                    }
                    ?: fallback
            }
            else -> fallback
        }
    } catch (e: Exception) {
        fallback
    }
}
