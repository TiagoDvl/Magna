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
        // The list endpoint does not carry it. It is filled in later, per committee, and the
        // insert is an upsert precisely so that this null does not overwrite what is stored.
        dataInicio = null,
    )
}