package com.tick.magna.data.source.remote.dto

import com.tick.magna.data.domain.MembroComissao
import kotlinx.serialization.Serializable

@Serializable
data class MembroOrgaoDto(
    val id: String,
    val nome: String,
    /**
     * Null in 58 of the 138 rows the CCJC returns for the 56th legislature, and never null on
     * the current one. The party of a past term is filled in from the local table instead; see
     * `OrgaosRepository.withPartidoFromLocal`.
     */
    val siglaPartido: String? = null,
    val siglaUf: String? = null,
    val urlFoto: String? = null,
    val titulo: String,
    val codTitulo: Int,
    val dataInicio: String,
    val dataFim: String? = null,
)

fun MembroOrgaoDto.toDomain(): MembroComissao {
    return MembroComissao(
        deputadoId = id,
        nome = nome,
        siglaPartido = siglaPartido,
        siglaUf = siglaUf,
        urlFoto = urlFoto,
        titulo = titulo,
        codTitulo = codTitulo,
        dataInicio = dataInicio,
        dataFim = dataFim,
    )
}
