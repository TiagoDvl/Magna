package com.tick.magna.data.source.remote.dto

import com.tick.magna.Partido
import com.tick.magna.data.domain.Deputado
import kotlinx.serialization.Serializable
import com.tick.magna.Deputado as DeputadoEntity

@Serializable
data class DeputadoDto(
    val id: String,
    val uri: String,
    val nome: String,
    val siglaPartido: String?,
    val uriPartido: String?,
    val siglaUf: String,
    val idLegislatura: Int,
    val urlFoto: String,
    val email: String?
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

fun DeputadoDto.toLocal(legislaturaId: String, partido: Partido): DeputadoEntity {
    return DeputadoEntity(
        id = id,
        legislaturaId = legislaturaId,
        partidoId = partido.id,
        last_seen = 0,
        name = nome,
        uf = siglaUf,
        profile_picture = urlFoto,
        email = email
    )
}