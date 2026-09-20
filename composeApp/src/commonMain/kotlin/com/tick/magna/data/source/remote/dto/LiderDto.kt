package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.Serializable

/**
 * The deputado leading a party bench.
 *
 * Every field is nullable because the register answers with a `lider` object full of nulls
 * rather than with no object at all: the DC and the PSC of the 57th have no leader and say so
 * that way. Non-null fields here failed the whole party request for both of them.
 */
@Serializable
data class LiderDto(
    val uri: String? = null,
    val nome: String? = null,
    val siglaPartido: String? = null,
    val uriPartido: String? = null,
    val uf: String? = null,
    val idLegislatura: Int? = null,
    val urlFoto: String? = null
)
