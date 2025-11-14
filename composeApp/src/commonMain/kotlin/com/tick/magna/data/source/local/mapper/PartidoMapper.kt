package com.tick.magna.data.source.local.mapper

import com.tick.magna.data.domain.Lider
import com.tick.magna.data.domain.Partido
import com.tick.magna.Partido as PartidoEntity

fun PartidoEntity.toDomain(): Partido {
    return Partido(
        id = id.toInt(),
        sigla = this.sigla,
        nome = this.nome,
        situacao = situacao,
        totalMembros = totalMembros.toInt(),
        dataStatus = situacao,
        lider = Lider(nome = liderDeputadoId, "", ""),
        urlLogo = this.logo,
        urlWebSite = website
    )
}
