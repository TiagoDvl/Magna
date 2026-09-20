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

/**
 * @param emExercicio whether this deputado held a seat on the term's reference date. Null when
 * that could not be measured, which the UI draws as nothing rather than as "no".
 */
fun DeputadoDto.toLocal(legislaturaId: String, emExercicio: Boolean?): DeputadoEntity {
    return DeputadoEntity(
        id = id,
        legislaturaId = legislaturaId,
        partido = siglaPartido,
        name = nome,
        uf = siglaUf,
        profile_picture = urlFoto,
        email = email,
        emExercicio = emExercicio?.let { if (it) 1L else 0L },
    )
}
