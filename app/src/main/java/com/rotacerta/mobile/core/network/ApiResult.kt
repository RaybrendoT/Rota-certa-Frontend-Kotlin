package com.rotacerta.mobile.core.network

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.Response

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Failure(val message: String) : ApiResult<Nothing>
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data))
    is ApiResult.Failure -> this
}

const val CONNECTION_ERROR = "Não foi possível conectar ao servidor. Verifique sua internet."

/** Executa uma chamada Retrofit e traduz sucesso/erro HTTP/falha de rede em [ApiResult]. */
suspend fun <T> apiCall(fallback: String, block: suspend () -> Response<T>): ApiResult<T> = try {
    val response = block()
    val body = response.body()
    when {
        response.isSuccessful && body != null -> ApiResult.Success(body)
        // Response<Unit> com corpo vazio (204/202) também é sucesso.
        response.isSuccessful -> @Suppress("UNCHECKED_CAST") ApiResult.Success(Unit as T)
        else -> ApiResult.Failure(response.errorMessage(fallback))
    }
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    ApiResult.Failure(CONNECTION_ERROR)
}

private val errorJson = Json { ignoreUnknownKeys = true }

/**
 * Extrai uma mensagem legível dos erros do DRF:
 * {"detail": "..."}, {"campo": ["..."]} ou {"non_field_errors": ["..."]}.
 */
fun Response<*>.errorMessage(fallback: String): String {
    val body = runCatching { errorBody()?.string() }.getOrNull()
    return parseDrfError(body) ?: fallback
}

fun parseDrfError(body: String?): String? {
    if (body.isNullOrBlank()) return null
    val element = runCatching { errorJson.parseToJsonElement(body) }.getOrNull() as? JsonObject ?: return null
    (element["detail"] as? JsonPrimitive)?.let { return it.content }
    val (field, value) = element.entries.firstOrNull() ?: return null
    val message = when (value) {
        is JsonArray -> value.firstOrNull()?.let { runCatching { it.jsonPrimitive.content }.getOrNull() }
        is JsonPrimitive -> value.content
        else -> null
    } ?: return null
    return if (field == "non_field_errors" || field == "detail") message else "$field: $message"
}
