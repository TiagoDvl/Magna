package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class VotacaoDetailDto(
    val id: String,
    val dataHoraRegistro: String?,
    val descricao: String,
    val aprovacao: Int,
    val idEvento: String?,
    val proposicoesAfetadas: List<ProposicoesAfetadasDto>,
    /**
     * The rapporteur's opinion, which is the only part of a committee vote with any substance
     * in it. It was already arriving in every response and being dropped on the floor.
     */
    val ultimaApresentacaoProposicao: UltimaApresentacaoProposicaoDto? = null,
)

/**
 * Optional throughout: a vote with no proposition behind it has no presentation either, and
 * the field is absent rather than empty when that happens.
 */
@Serializable
data class UltimaApresentacaoProposicaoDto(
    val descricao: String? = null,
)
