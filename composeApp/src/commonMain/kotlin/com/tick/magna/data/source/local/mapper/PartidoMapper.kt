package com.tick.magna.data.source.local.mapper

import com.tick.magna.data.domain.Lider
import com.tick.magna.data.domain.Partido
import com.tick.magna.GetPartidos
import com.tick.magna.Partido as PartidoEntity

fun PartidoEntity.toDomain(): Partido {
    return Partido(
        id = id.toInt(),
        sigla = this.sigla,
        nome = this.nome,
        situacao = situacao,
        totalMembros = totalMembros?.toInt(),
        dataStatus = situacao,
        lider = liderDeputadoId?.let { Lider(nome = it, "", "") },
        urlLogo = this.logo,
        urlWebSite = website
    )
}

/** The list row, which carries the local count and the favourite flag the query joins in. */
fun GetPartidos.toDomain(): Partido {
    return Partido(
        id = id.toInt(),
        sigla = sigla,
        nome = nome,
        situacao = situacao,
        totalMembros = totalMembros?.toInt(),
        dataStatus = situacao,
        lider = liderDeputadoId?.let { Lider(nome = it, "", "") },
        urlLogo = logo,
        urlWebSite = website,
        deputados = deputadoCount.toInt(),
        isFavorito = isFavorito,
    )
}
