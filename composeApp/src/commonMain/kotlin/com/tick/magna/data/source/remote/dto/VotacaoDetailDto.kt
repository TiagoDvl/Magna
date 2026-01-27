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
)
