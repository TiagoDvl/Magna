package com.tick.magna.data.source.local.mapper

import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.domain.Proposicao
import com.tick.magna.Proposicao as ProposicaoEntity

fun ProposicaoEntity.toDomain(deputadosAutores: List<Deputado>): Proposicao {
    return Proposicao(
        id = id,
        type = codTipo.orEmpty(),
        ementa = ementa.orEmpty(),
        // Tolerant on purpose: the API mixes date and date-time here too, and a strict
        // parse would empty the whole section over one odd record.
        dataApresentacao = dataApresentacao.orEmpty().toDisplayDate(),
        autores = deputadosAutores,
        url = url
    )
}