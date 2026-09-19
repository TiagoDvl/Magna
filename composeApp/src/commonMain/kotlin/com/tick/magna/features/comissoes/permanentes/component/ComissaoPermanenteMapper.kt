package com.tick.magna.features.comissoes.permanentes.component

import com.tick.magna.data.domain.Orgao
import com.tick.magna.features.comissoes.permanentes.component.domain.ComissaoPermanente

/**
 * Shared by the Home carousel and the full list, which used to disagree by having only one of
 * them. A committee with no name is dropped rather than rendered as a blank card.
 */
fun List<Orgao>.toComissoesPermanentes(): List<ComissaoPermanente> = mapNotNull { orgao ->
    if (orgao.nome == null || orgao.nomeResumido == null) return@mapNotNull null

    ComissaoPermanente(
        comissaoPermanenteId = orgao.id,
        nomeResumido = orgao.nomeResumido,
        nome = orgao.nome,
        votacoes = orgao.votacoes,
    )
}
