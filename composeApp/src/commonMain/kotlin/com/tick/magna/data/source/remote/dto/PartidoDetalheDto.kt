package com.tick.magna.data.source.remote.dto

import com.tick.magna.data.domain.Lider
import com.tick.magna.data.domain.Partido
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

fun PartidoDetalheDto.toDomain(): Partido {
    return Partido(
        id = id,
        sigla = sigla,
        nome = nome,
        situacao = status?.situacao ?: "Desconhecido",
        totalMembros = status?.totalMembros ?: 0,
        dataStatus = status?.data ?: "",
        lider = status?.lider?.let {
            Lider(
                nome = it.nome,
                uf = it.uf,
                urlFoto = it.urlFoto
            )
        },
        numeroEleitoral = numeroEleitoral,
        urlLogo = urlLogo,
        urlWebSite = urlWebSite,
        urlFacebook = urlFacebook
    )
}