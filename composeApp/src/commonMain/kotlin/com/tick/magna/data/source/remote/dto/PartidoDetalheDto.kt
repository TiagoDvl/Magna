package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PartidoDetalheDto(
    val id: Int,
    val sigla: String,
    val nome: String,
    val uri: String,
    val status: StatusPartidoDto?,
    val numeroEleitoral: String? = null,
    val urlLogo: String? = null,
    val urlWebSite: String? = null,
    val urlFacebook: String? = null
)
