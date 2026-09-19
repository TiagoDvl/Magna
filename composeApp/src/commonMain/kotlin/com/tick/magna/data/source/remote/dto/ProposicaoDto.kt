package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.Serializable

/**
 * A proposition as it appears in a list. Only the id and the type are read; the rest is
 * optional so one incomplete record cannot empty the home screen section.
 */
@Serializable
data class ProposicaoDto(
    val id: Int,
    val codTipo: Int = 0,
    val ementa: String? = null,
    val dataApresentacao: String? = null,
    // "PL 1589/2026" is the name a proposition is looked up by, and both halves come free in
    // the listing. The card used to show the sigla alone, which every card of a type shares.
    val numero: Int? = null,
    val ano: Int? = null,
)
