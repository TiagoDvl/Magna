package com.tick.magna.data.source.remote.dto

import com.tick.magna.data.domain.Deputado
import kotlinx.serialization.Serializable

@Serializable
data class DeputadoDto(
    val id: Int,
    val uri: String,
    val nome: String,
    val siglaPartido: String,
    val uriPartido: String,
    val siglaUf: String,
    val idLegislatura: Int,
    val urlFoto: String,
    val email: String?
)

fun DeputadoDto.toDomain(): Deputado {
    return Deputado(
        id = this.id,
        nome = this.nome,
        partido = this.siglaPartido,
        uf = this.siglaUf,
        fotoUrl = this.urlFoto,
        email = this.email ?: ""
    )
}