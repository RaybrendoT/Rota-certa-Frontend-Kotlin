package com.rotacerta.mobile.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** GET/PUT /me/address/: endereço residencial atual e o ponto de embarque mais próximo dele. */
@Serializable
data class MyAddressDto(
    val address: HomeAddressDto? = null,
    @SerialName("nearest_point") val nearestPoint: NearestPointDto? = null,
    /** Endereço enviado pelo app aguardando aprovação do gestor (até lá vale o [address]). */
    val pending: HomeAddressDto? = null,
    /** Último envio recusado pelo gestor, com o motivo em reviewNote. */
    val rejected: HomeAddressDto? = null,
    @SerialName("changes_left_this_week") val changesLeftThisWeek: Int = 0,
    /** Só na resposta do PUT ("Endereço salvo. Seu ponto de embarque..."). */
    val message: String? = null,
)

@Serializable
data class HomeAddressDto(
    val street: String,
    val number: String,
    val complement: String = "",
    val neighborhood: String = "",
    val city: String,
    val state: String,
    @SerialName("postal_code") val postalCode: String = "",
    val latitude: Double,
    val longitude: Double,
    /** panel | gps | typed */
    val source: String,
    @SerialName("source_label") val sourceLabel: String = "",
    /** pending | approved | rejected (vazio nos cadastrados pelo painel). */
    @SerialName("review_status") val reviewStatus: String = "",
    @SerialName("review_note") val reviewNote: String = "",
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class NearestPointDto(
    val id: Long,
    val name: String,
    @SerialName("distance_m") val distanceMeters: Int,
)

/** Campos do endereço como o colaborador confirma (rua, número...). */
@Serializable
data class AddressFieldsDto(
    val street: String = "",
    val number: String = "",
    val neighborhood: String = "",
    val city: String = "",
    val state: String = "",
    @SerialName("postal_code") val postalCode: String = "",
)

/** POST /me/address/preview/ — mode "gps" (lat/lng/precisão) ou "typed" (CEP, rua, número...). */
@Serializable
data class AddressPreviewRequest(
    val mode: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy: Float? = null,
    @SerialName("is_mock") val isMock: Boolean = false,
    @SerialName("postal_code") val postalCode: String? = null,
    val street: String? = null,
    val number: String? = null,
    val neighborhood: String? = null,
    val city: String? = null,
    val state: String? = null,
)

@Serializable
data class AddressPreviewDto(
    val source: String,
    val latitude: Double,
    val longitude: Double,
    @SerialName("accuracy_m") val accuracyMeters: Float? = null,
    /** false = o Google achou só a rua ou o bairro; o alfinete precisa ser ajustado. */
    val exact: Boolean = true,
    val address: AddressFieldsDto,
    val warnings: List<String> = emptyList(),
    @SerialName("nearest_point") val nearestPoint: NearestPointDto? = null,
)

/** PUT /me/address/ — endereço confirmado com o alfinete na posição final. */
@Serializable
data class SaveAddressRequest(
    val source: String,
    val street: String,
    val number: String,
    val complement: String,
    val neighborhood: String,
    val city: String,
    val state: String,
    @SerialName("postal_code") val postalCode: String,
    val latitude: Double,
    val longitude: Double,
    @SerialName("gps_latitude") val gpsLatitude: Double? = null,
    @SerialName("gps_longitude") val gpsLongitude: Double? = null,
    @SerialName("gps_accuracy") val gpsAccuracy: Float? = null,
    @SerialName("gps_is_mock") val gpsIsMock: Boolean = false,
)

/** GET /me/address/cep/{cep}/ */
@Serializable
data class CepDto(
    @SerialName("postal_code") val postalCode: String,
    val street: String = "",
    val neighborhood: String = "",
    val city: String = "",
    val state: String = "",
)
