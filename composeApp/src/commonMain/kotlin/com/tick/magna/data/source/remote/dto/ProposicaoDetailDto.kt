package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProposicaoDetailDto(
    val id: String,
    val siglaTipo: String = "",
    val numero: Int = 0,
    val ano: Int = 0,
    val ementa: String = "",
    val dataApresentacao: String = "",
    val urlInteiroTeor: String? = null,
    val statusProposicao: StatusProposicaoDto? = null,
)
