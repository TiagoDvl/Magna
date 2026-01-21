package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EventoDetailsPautaDto(
    val ordem: String,
    val regime: String,
    val relator: DeputadoDto?,
    @SerialName("proposicao_") val proposicao: ProposicaoDto,
    val textoParecer: String?,
    val situacaoItem: String,
)
