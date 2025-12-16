package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProposicaoDto(
    val id: Int,
    val uri: String,
    val siglaTipo: String,
    val codTipo: Int,
    val numero: Int,
    val ano: Int,
    val ementa: String,
    val dataApresentacao: String
)
