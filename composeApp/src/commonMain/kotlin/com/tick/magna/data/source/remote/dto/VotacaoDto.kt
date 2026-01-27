package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class VotacaoDto(
    val id: String,
    val uriEvento: String?,
    val uriProposicaoObjeto: String?,
)