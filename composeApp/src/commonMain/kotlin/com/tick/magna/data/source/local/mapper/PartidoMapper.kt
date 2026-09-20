package com.tick.magna.data.source.local.mapper

import com.tick.magna.data.domain.Lider
import com.tick.magna.data.domain.Partido
import com.tick.magna.data.domain.PartidoDetail
import com.tick.magna.GetPartidos
import com.tick.magna.Partido as PartidoEntity

fun PartidoEntity.toDomain(): Partido {
    return Partido(
        id = id.toInt(),
        sigla = this.sigla,
        nome = this.nome,
        situacao = situacao,
        totalMembros = totalMembros?.toInt(),
        dataStatus = dataStatus,
        lider = lider(),
        urlLogo = this.logo,
        urlWebSite = website,
        cor = cor?.toInt(),
    )
}

/**
 * The header of the party screen, read from the row the sync wrote.
 *
 * It used to be built straight from `/partidos/{id}` on every open, which made the header a
 * loading state every time and an error state for good on any term but the current one — that
 * endpoint answers 400 for an older legislatura. Everything it returns is stored now, so this
 * is a read.
 */
fun PartidoEntity.toDetail(): PartidoDetail {
    return PartidoDetail(
        id = id.toInt(),
        sigla = sigla,
        nome = nome,
        urlLogo = logo,
        totalPosse = totalPosse?.toInt(),
        totalMembros = totalMembros?.toInt(),
        situacao = situacao,
        dataStatus = dataStatus,
        lider = lider(),
        cor = cor?.toInt(),
    )
}

/**
 * The bench leader, or nobody.
 *
 * This used to hand the leader's *id* over as their name, because the only column the table
 * had was `liderDeputadoId` and something had to go in the field. Two parties of the 57th have
 * no leader at all, so absent is a real answer and not a failure.
 */
private fun PartidoEntity.lider(): Lider? {
    val nome = liderNome ?: return null

    return Lider(
        id = liderDeputadoId,
        nome = nome,
        uf = liderUf.orEmpty(),
        urlFoto = liderFoto.orEmpty(),
    )
}

/** The list row, which carries the local count and the chosen position the query joins in. */
fun GetPartidos.toDomain(): Partido {
    return Partido(
        id = id.toInt(),
        sigla = sigla,
        nome = nome,
        situacao = situacao,
        totalMembros = totalMembros?.toInt(),
        dataStatus = dataStatus,
        lider = liderNome?.let {
            Lider(liderDeputadoId, it, liderUf.orEmpty(), liderFoto.orEmpty())
        },
        urlLogo = logo,
        urlWebSite = website,
        deputados = deputadoCount.toInt(),
        bancada = bancadaCount.toInt(),
        posicao = posicao?.toInt(),
        cor = cor?.toInt(),
    )
}
