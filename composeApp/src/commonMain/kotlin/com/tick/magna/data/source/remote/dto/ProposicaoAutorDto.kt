package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.Serializable

/**
 * One signature on a proposition.
 *
 * [nome] and [tipo] were being thrown away, and the card paid for it: it resolved [uri] to a
 * deputado in the local table and drew an avatar, so the third of PLs signed by an orgao —
 * and every MSC, which the Executive signs — resolved to nobody and rendered as an empty row.
 * The name is right here.
 */
@Serializable
data class ProposicaoAutorDto(
    val uri: String,
    val ordemAssinatura: Int,
    val nome: String? = null,
    /** "Deputado(a)", "Orgao do Poder Executivo", "COMISSAO PERMANENTE". */
    val tipo: String? = null,
)
