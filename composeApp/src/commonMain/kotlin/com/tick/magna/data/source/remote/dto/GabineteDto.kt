package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GabineteDto(
    @SerialName("predio")
    val predio: String? = null,
    @SerialName("sala")
    val sala: String? = null,
    @SerialName("telefone")
    val telefone: String? = null,
    @SerialName("email")
    val email: String? = null,
)
