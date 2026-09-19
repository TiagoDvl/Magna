package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class VotacaoDto(
    val id: String,
    val uri: String? = null,
    val uriEvento: String? = null,
    val uriProposicaoObjeto: String? = null,
    val descricao: String? = null,
    val dataHoraRegistro: String? = null,
    val siglaOrgao: String? = null,
    /** 1 when it passed. The listing carries it; only the detail was ever read for it. */
    val aprovacao: Int? = null,
    val proposicoesAfetadas: List<ProposicoesAfetadasDto> = emptyList(),
)