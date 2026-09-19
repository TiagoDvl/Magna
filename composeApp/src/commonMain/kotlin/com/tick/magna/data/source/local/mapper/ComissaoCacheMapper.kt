package com.tick.magna.data.source.local.mapper

import com.tick.magna.ComissaoMembro as ComissaoMembroEntity
import com.tick.magna.ComissaoVotacao as ComissaoVotacaoEntity
import com.tick.magna.ComissaoVotacaoProposicao as ComissaoVotacaoProposicaoEntity
import com.tick.magna.data.domain.MembroComissao
import com.tick.magna.data.domain.ProposicaoAfetada
import com.tick.magna.data.domain.Votacao
import com.tick.magna.data.source.local.dao.ComissaoConteudo

/**
 * Rebuilds the votes from the two tables they are stored in.
 *
 * The rows arrive already ordered — the votes by timestamp, the propositions by the position
 * the API listed them in — so this only has to group, and grouping preserves order.
 */
internal fun List<ComissaoVotacaoEntity>.toDomain(
    proposicoes: List<ComissaoVotacaoProposicaoEntity>,
): List<Votacao> {
    val byVotacao = proposicoes.groupBy { it.votacaoId }

    return map { votacao ->
        Votacao(
            id = votacao.votacaoId,
            dataHoraRegistro = votacao.dataHoraRegistro,
            descricao = votacao.descricao,
            aprovacao = votacao.aprovacao == APPROVED,
            proposicoes = byVotacao[votacao.votacaoId].orEmpty().map { proposicao ->
                ProposicaoAfetada(
                    id = proposicao.proposicaoId,
                    rotulo = proposicao.rotulo,
                    ementa = proposicao.ementa,
                )
            },
            parecer = votacao.parecer,
            idEvento = votacao.idEvento,
        )
    }
}

internal fun List<Votacao>.toVotacaoEntities(
    orgaoId: String,
    legislaturaId: String,
): List<ComissaoVotacaoEntity> = map { votacao ->
    ComissaoVotacaoEntity(
        orgaoId = orgaoId,
        legislaturaId = legislaturaId,
        votacaoId = votacao.id,
        dataHoraRegistro = votacao.dataHoraRegistro,
        descricao = votacao.descricao,
        aprovacao = if (votacao.aprovacao) APPROVED else NOT_APPROVED,
        parecer = votacao.parecer,
        idEvento = votacao.idEvento,
    )
}

internal fun List<Votacao>.toProposicaoEntities(): List<ComissaoVotacaoProposicaoEntity> =
    flatMap { votacao ->
        votacao.proposicoes.mapIndexed { index, proposicao ->
            ComissaoVotacaoProposicaoEntity(
                votacaoId = votacao.id,
                proposicaoId = proposicao.id,
                ordem = index.toLong(),
                rotulo = proposicao.rotulo,
                ementa = proposicao.ementa,
            )
        }
    }

internal fun List<ComissaoMembroEntity>.toMembrosDomain(): List<MembroComissao> = map { membro ->
    MembroComissao(
        deputadoId = membro.deputadoId,
        nome = membro.nome,
        siglaPartido = membro.siglaPartido,
        siglaUf = membro.siglaUf,
        urlFoto = membro.urlFoto,
        titulo = membro.titulo,
        codTitulo = membro.codTitulo.toInt(),
        dataInicio = membro.dataInicio,
        dataFim = membro.dataFim,
    )
}

internal fun List<MembroComissao>.toMembroEntities(
    orgaoId: String,
    legislaturaId: String,
    fonte: ComissaoConteudo,
): List<ComissaoMembroEntity> = map { membro ->
    ComissaoMembroEntity(
        orgaoId = orgaoId,
        legislaturaId = legislaturaId,
        fonte = fonte.name,
        deputadoId = membro.deputadoId,
        dataInicio = membro.dataInicio,
        nome = membro.nome,
        siglaPartido = membro.siglaPartido,
        siglaUf = membro.siglaUf,
        urlFoto = membro.urlFoto,
        titulo = membro.titulo,
        codTitulo = membro.codTitulo.toLong(),
        dataFim = membro.dataFim,
    )
}

private const val APPROVED = 1L
private const val NOT_APPROVED = 0L
