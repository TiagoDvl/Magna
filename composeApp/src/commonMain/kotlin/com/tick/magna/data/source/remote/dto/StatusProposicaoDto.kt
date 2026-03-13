package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class StatusProposicaoDto(
    val idSituacao: Int? = null,
    val descricaoSituacao: String? = null,
    val despacho: String? = null,
    val siglaOrgao: String? = null,
)
