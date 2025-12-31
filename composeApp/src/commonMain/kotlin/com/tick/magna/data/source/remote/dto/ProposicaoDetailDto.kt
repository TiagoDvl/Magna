package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProposicaoDetailDto(
    val id: String,
    val uriAutores: String?
)
