package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProposicaoAutorDto(
    val uri: String,
    val ordemAssinatura: Int
)
