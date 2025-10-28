package com.tick.magna.data.source.remote.dto

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
        id = this.id,
        name = this.nome,
        partido = this.siglaPartido.orEmpty(),
        uf = this.siglaUf,
        profilePicture = this.urlFoto,
        email = this.email ?: ""
    )
}

fun DeputadoDto.toLocal(): DeputadoEntity {
    return DeputadoEntity(
        id = this.id,
        legislaturaId = "",
        partidoId = null,
        favorite = 0,
        name = this.nome,
        uf = this.siglaUf,
        profile_picture = this.urlFoto,
        email = this.email
    )
}