package com.tick.magna.data.source.remote.dto

import com.tick.magna.data.domain.Deputado
import kotlinx.serialization.Serializable
import com.tick.magna.Deputado as DeputadoEntity

/**
 * A deputado as it appears in a list. Only the id is required; a missing photo or state
 * should cost that one field, not the whole list.
 */
@Serializable
data class DeputadoDto(
    val id: String,
    val nome: String = "",
    val siglaPartido: String? = null,
    val siglaUf: String? = null,
    val urlFoto: String? = null,
    val email: String? = null,
)

fun DeputadoDto.toDomain(): Deputado {
    return Deputado(
        id = id,
        name = nome,
        partido = siglaPartido.orEmpty(),
        uf = siglaUf,
        profilePicture = urlFoto,
        email = email ?: ""
    )
}

fun DeputadoDto.toLocal(legislaturaId: String): DeputadoEntity {
    return DeputadoEntity(
        id = id,
        legislaturaId = legislaturaId,
        partido = siglaPartido,
        last_seen = 0,
        name = nome,
        uf = siglaUf,
        profile_picture = urlFoto,
        email = email
    )
}
