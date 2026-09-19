package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.Serializable

/**
 * A subject a proposition was classified under, from a vocabulary of about 25.
 *
 * It arrives late: of 30 propositions filed within twenty days, none had a tema, while 29 of
 * 30 filed six months earlier did. So it is decoration the card shows when it exists, never
 * something the card is built around.
 */
@Serializable
data class ProposicaoTemaDto(
    val codTema: Int? = null,
    val tema: String? = null,
)
