package com.tick.magna.data.source.remote.dto

import com.tick.magna.data.domain.ProposicaoAfetada
import kotlinx.serialization.Serializable

@Serializable
data class ProposicoesAfetadasDto(
    val id: String,
    val uri: String? = null,
    val siglaTipo: String? = null,
    val numero: Int? = null,
    val ano: Int? = null,
    val ementa: String = "",
)

/**
 * Builds the short name people recognise — "PL 4770/2023" — out of three fields the API has
 * always returned and the app has always discarded, leaving a bare ementa with nothing to call
 * it. Null when any of the three is missing, because half a label is worse than none.
 */
fun ProposicoesAfetadasDto.toDomain(): ProposicaoAfetada {
    val rotulo = if (siglaTipo != null && numero != null && ano != null) {
        "$siglaTipo $numero/$ano"
    } else {
        null
    }

    return ProposicaoAfetada(id = id, rotulo = rotulo, ementa = ementa)
}
