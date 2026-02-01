package com.tick.magna.data.source.local.mapper

import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.domain.Proposicao
import kotlinx.datetime.LocalDateTime
import com.tick.magna.Proposicao as ProposicaoEntity

fun ProposicaoEntity.toDomain(deputadosAutores: List<Deputado>): Proposicao {
    return Proposicao(
        id = id,
        type = codTipo.orEmpty(),
        ementa = ementa.orEmpty(),
        dataApresentacao = formatter.format(LocalDateTime.parse(dataApresentacao.orEmpty())),
        autores = deputadosAutores,
        url = url
    )
}