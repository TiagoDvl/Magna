package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.Serializable

/**
 * One órgão in full. Only [dataInicio] is read today — it is the field the list endpoint
 * does not carry, and the reason this detail is fetched at all.
 *
 * [dataFim] is declared because its absence is the finding: no comissão permanente has one.
 */
@Serializable
data class OrgaoDetalheDto(
    val id: String = "",
    val sigla: String = "",
    val nome: String = "",
    val dataInicio: String? = null,
    val dataFim: String? = null,
)
