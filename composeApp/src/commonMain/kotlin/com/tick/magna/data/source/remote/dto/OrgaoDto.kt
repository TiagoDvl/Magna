package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.Serializable
import com.tick.magna.Orgao as OrgaoEntity

@Serializable
data class OrgaoDto(
    val id: String,
    val sigla: String,
    val nome: String,
    val nomeResumido: String,
)

fun OrgaoDto.toLocal(): OrgaoEntity {
    return OrgaoEntity(
        id = this.id,
        sigla = this.sigla,
        nome = this.nome,
        nomeResumido = this.nomeResumido,
    )
}