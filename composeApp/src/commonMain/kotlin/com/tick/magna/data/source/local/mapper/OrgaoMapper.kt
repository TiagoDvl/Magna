package com.tick.magna.data.source.local.mapper

import com.tick.magna.data.domain.Orgao
import com.tick.magna.Orgao as OrgaoEntity

fun OrgaoEntity.toDomain(): Orgao {
    return Orgao(
        id = id,
        sigla = sigla,
        nome = nome,
        nomeResumido = nomeResumido
    )
}