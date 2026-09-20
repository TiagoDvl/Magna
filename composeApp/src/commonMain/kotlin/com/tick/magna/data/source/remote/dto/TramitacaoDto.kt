package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.Serializable

/** One step of a proposition's passage through the house. */
@Serializable
data class TramitacaoDto(
    val dataHora: String? = null,
    val sequencia: Int? = null,
    val siglaOrgao: String? = null,
    val descricaoTramitacao: String? = null,
    val descricaoSituacao: String? = null,
    val despacho: String? = null,
)
