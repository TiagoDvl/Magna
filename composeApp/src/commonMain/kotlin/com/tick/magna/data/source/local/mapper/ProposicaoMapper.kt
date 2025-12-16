package com.tick.magna.data.source.local.mapper

import com.tick.magna.data.domain.Proposicao
import com.tick.magna.data.domain.deputadosMock
import com.tick.magna.Proposicao as ProposicaoEntity

fun ProposicaoEntity.toDomain(): Proposicao {
    return Proposicao(
        id = id,
        type = codTipo.orEmpty(),
        ementa = ementa.orEmpty(),
        dataApresentacao = dataApresentacao.orEmpty(),
        autores = deputadosMock.subList(0, 3),
        url = ""
    )
}