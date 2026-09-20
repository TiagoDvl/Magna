package com.tick.magna.data.source.local.dao

import com.tick.magna.ComissaoCacheQueries
import com.tick.magna.ComissaoMembro
import com.tick.magna.ComissaoMembroQueries
import com.tick.magna.ComissaoVotacao
import com.tick.magna.ComissaoVotacaoProposicao
import com.tick.magna.ComissaoVotacaoQueries
import com.tick.magna.MagnaDatabase
import com.tick.magna.SelectComissoesDosDeputados
import com.tick.magna.data.dispatcher.DispatcherInterface
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.flow.Flow

class ComissaoCacheDao(
    private val database: MagnaDatabase,
    private val votacaoQueries: ComissaoVotacaoQueries,
    private val membroQueries: ComissaoMembroQueries,
    private val cacheQueries: ComissaoCacheQueries,
    private val dispatcher: DispatcherInterface,
) : ComissaoCacheDaoInterface {

    override suspend fun getFetchedAt(
        orgaoId: String,
        legislaturaId: String,
        conteudo: ComissaoConteudo,
    ): Long? {
        return cacheQueries
            .selectComissaoCache(orgaoId, legislaturaId, conteudo.name)
            .executeAsOneOrNull()
    }

    override suspend fun getVotacoes(orgaoId: String, legislaturaId: String): List<ComissaoVotacao> {
        return votacaoQueries.selectComissaoVotacoes(orgaoId, legislaturaId).executeAsList()
    }

    override suspend fun getVotacaoProposicoes(
        orgaoId: String,
        legislaturaId: String,
    ): List<ComissaoVotacaoProposicao> {
        return votacaoQueries.selectComissaoVotacaoProposicoes(orgaoId, legislaturaId).executeAsList()
    }

    override suspend fun saveVotacoes(
        orgaoId: String,
        legislaturaId: String,
        votacoes: List<ComissaoVotacao>,
        proposicoes: List<ComissaoVotacaoProposicao>,
        fetchedAt: Long,
    ) {
        database.transaction {
            // The propositions first: they are found through the votes, so deleting the votes
            // first would leave nothing to find them by.
            votacaoQueries.deleteComissaoVotacaoProposicoes(orgaoId, legislaturaId)
            votacaoQueries.deleteComissaoVotacoes(orgaoId, legislaturaId)

            votacoes.forEach { votacaoQueries.insertComissaoVotacao(it) }
            proposicoes.forEach { votacaoQueries.insertComissaoVotacaoProposicao(it) }

            cacheQueries.insertComissaoCache(
                orgaoId,
                legislaturaId,
                ComissaoConteudo.VOTACOES.name,
                fetchedAt,
            )
        }
    }

    override suspend fun getMembros(
        orgaoId: String,
        legislaturaId: String,
        fonte: ComissaoConteudo,
    ): List<ComissaoMembro> {
        return when (fonte) {
            ComissaoConteudo.PRESIDENCIA ->
                membroQueries.selectComissaoPresidencia(orgaoId, legislaturaId).executeAsList()

            else ->
                membroQueries.selectComissaoComposicao(orgaoId, legislaturaId).executeAsList()
        }
    }

    override fun observeComissoesDosDeputados(
        legislaturaId: String,
    ): Flow<List<SelectComissoesDosDeputados>> {
        return membroQueries
            .selectComissoesDosDeputados(legislaturaId)
            .asFlow()
            .mapToList(dispatcher.io)
    }

    override suspend fun saveMembros(
        orgaoId: String,
        legislaturaId: String,
        fonte: ComissaoConteudo,
        membros: List<ComissaoMembro>,
        fetchedAt: Long,
    ) {
        database.transaction {
            membroQueries.deleteComissaoMembros(orgaoId, legislaturaId, fonte.name)
            membros.forEach { membroQueries.insertComissaoMembro(it) }

            cacheQueries.insertComissaoCache(orgaoId, legislaturaId, fonte.name, fetchedAt)
        }
    }
}
