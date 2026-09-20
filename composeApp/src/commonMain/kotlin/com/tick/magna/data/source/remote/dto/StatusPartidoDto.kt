package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.Serializable

/**
 * A party's standing in a term.
 *
 * [data] and [situacao] are nullable because they are null in the register: the DC and the PSC
 * of the 57th both answer with `"data": null`, and a non-null field there failed the whole
 * request for those two parties with "Unexpected 'null' value instead of string literal".
 * A party with no status date is a party the register has not restated, not a parse error.
 */
@Serializable
data class StatusPartidoDto(
    val data: String? = null,
    val situacao: String? = null,
    val totalPosse: Int? = null,
    val totalMembros: Int? = null,
    val uriMembros: String? = null,
    val lider: LiderDto? = null
)
