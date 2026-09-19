package com.tick.magna.data.source.local.mapper

import com.tick.magna.SelectOrgaosByAtividade
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

/**
 * The list row, which carries the activity count the query joins in.
 *
 * The query answers -1 for a committee this term has not measured yet, and that becomes null
 * here: "not counted" and "counted zero" are different things, and only one of them is a fact
 * about the committee.
 */
fun SelectOrgaosByAtividade.toDomain(): Orgao {
    return Orgao(
        id = id,
        sigla = sigla,
        nome = nome,
        nomeResumido = nomeResumido,
        votacoes = votacoes.takeIf { it >= 0 }?.toInt(),
    )
}
