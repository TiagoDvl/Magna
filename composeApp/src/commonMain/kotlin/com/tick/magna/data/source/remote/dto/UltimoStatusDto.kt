package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UltimoStatusDto(
    @SerialName("id")
    val id: Long,

    @SerialName("uri")
    val uri: String,

    @SerialName("nome")
    val nome: String,

    @SerialName("siglaPartido")
    val siglaPartido: String,

    @SerialName("uriPartido")
    val uriPartido: String? = null,

    @SerialName("siglaUf")
    val siglaUf: String,

    @SerialName("idLegislatura")
    val idLegislatura: Int,

    @SerialName("urlFoto")
    val urlFoto: String,

    @SerialName("email")
    val email: String? = null,

    @SerialName("data")
    val data: String,

    @SerialName("nomeEleitoral")
    val nomeEleitoral: String,

    @SerialName("gabinete")
    val gabinete: GabineteDto,

    @SerialName("situacao")
    val situacao: String,

    @SerialName("condicaoEleitoral")
    val condicaoEleitoral: String,

    @SerialName("descricaoStatus")
    val descricaoStatus: String? = null
)