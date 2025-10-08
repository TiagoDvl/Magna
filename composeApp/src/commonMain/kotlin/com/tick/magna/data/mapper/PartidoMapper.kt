package com.tick.magna.data.mapper

import com.tick.magna.data.domain.Lider
import com.tick.magna.data.domain.Partido
import com.tick.magna.data.source.remote.dto.PartidoDetalheDto

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