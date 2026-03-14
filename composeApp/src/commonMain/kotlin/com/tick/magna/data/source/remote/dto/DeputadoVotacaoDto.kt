package com.tick.magna.data.source.remote.dto

import com.tick.magna.data.domain.DeputadoVotacao
import com.tick.magna.data.domain.ProposicaoVotada
import kotlinx.serialization.Serializable

@Serializable
data class DeputadoVotacaoDto(
    val id: String,
    val uri: String? = null,
    val data: String? = null,
    val dataHoraVoto: String? = null,
    val tipoVoto: String,
    val proposicoesVotadas: List<ProposicaoVotadaDto> = emptyList(),
    val votacao: VotacaoContextDto,
)

@Serializable
data class ProposicaoVotadaDto(
    val id: Long? = null,
    val uri: String? = null,
    val siglaTipo: String? = null,
    val codTipo: Int? = null,
    val numero: Int? = null,
    val ano: Int? = null,
    val ementa: String? = null,
)

@Serializable
data class VotacaoContextDto(
    val id: String? = null,
    val uri: String? = null,
    val uriEvento: String? = null,
    val uriProposicaoObjeto: String? = null,
    val descricao: String? = null,
    val data: String? = null,
    val dataHoraRegistro: String? = null,
    val siglaOrgao: String? = null,
)

fun DeputadoVotacaoDto.toDomain(): DeputadoVotacao {
    return DeputadoVotacao(
        id = id,
        dataHoraVoto = dataHoraVoto ?: data,
        tipoVoto = tipoVoto,
        descricao = votacao.descricao.orEmpty(),
        uriVotacao = votacao.uri,
        siglaOrgao = votacao.siglaOrgao,
        proposicoes = proposicoesVotadas.mapNotNull { prop ->
            val tipo = prop.siglaTipo ?: return@mapNotNull null
            val num = prop.numero ?: return@mapNotNull null
            val ano = prop.ano ?: return@mapNotNull null
            ProposicaoVotada(
                siglaTipo = tipo,
                numero = num,
                ano = ano,
                ementa = prop.ementa.orEmpty(),
                uri = prop.uri,
            )
        },
    )
}

fun VotacaoDto.toDeputadoVotacao(tipoVoto: String, dataRegistroVoto: String?): DeputadoVotacao {
    return DeputadoVotacao(
        id = id,
        dataHoraVoto = dataRegistroVoto ?: dataHoraRegistro,
        tipoVoto = tipoVoto,
        descricao = descricao.orEmpty(),
        uriVotacao = uri,
        siglaOrgao = siglaOrgao,
        proposicoes = proposicoesAfetadas.mapNotNull { prop ->
            val tipo = prop.siglaTipo ?: return@mapNotNull null
            val num = prop.numero ?: return@mapNotNull null
            val ano = prop.ano ?: return@mapNotNull null
            ProposicaoVotada(
                siglaTipo = tipo,
                numero = num,
                ano = ano,
                ementa = prop.ementa,
                uri = prop.uri,
            )
        },
    )
}
