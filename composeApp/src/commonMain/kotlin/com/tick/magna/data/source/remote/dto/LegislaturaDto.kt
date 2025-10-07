package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LegislaturaDto(
    val id: Int,
    val uri: String,
    val dataInicio: String,
    val dataFim: String
)
