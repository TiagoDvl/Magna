package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GabineteDto(
    @SerialName("nome")
    val nome: String,

    @SerialName("predio")
    val predio: String,

    @SerialName("sala")
    val sala: String,

    @SerialName("andar")
    val andar: String,

    @SerialName("telefone")
    val telefone: String,

    @SerialName("email")
    val email: String
)