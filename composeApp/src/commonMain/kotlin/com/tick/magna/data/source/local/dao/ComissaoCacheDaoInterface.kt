package com.tick.magna.data.source.local.dao

import com.tick.magna.ComissaoMembro
import com.tick.magna.ComissaoVotacao
import com.tick.magna.ComissaoVotacaoProposicao
import com.tick.magna.SelectComissoesDosDeputados
import kotlinx.coroutines.flow.Flow

interface ComissaoCacheDaoInterface {

    /**
     * When this content was last downloaded for this committee and term, or null if it never
     * was. Null and an empty list are different answers: the CASP has no votes.
     */
    suspend fun getFetchedAt(
        orgaoId: String,
        legislaturaId: String,
        conteudo: ComissaoConteudo,
    ): Long?

    suspend fun getVotacoes(orgaoId: String, legislaturaId: String): List<ComissaoVotacao>

    suspend fun getVotacaoProposicoes(
        orgaoId: String,
        legislaturaId: String,
    ): List<ComissaoVotacaoProposicao>

    /**
     * Replaces everything stored for this committee and term and stamps the fetch, in one
     * transaction. Replacing rather than merging, so a refresh that returns fewer votes does
     * not leave the ones that went away on the screen.
     */
    suspend fun saveVotacoes(
        orgaoId: String,
        legislaturaId: String,
        votacoes: List<ComissaoVotacao>,
        proposicoes: List<ComissaoVotacaoProposicao>,
        fetchedAt: Long,
    )

    suspend fun getMembros(
        orgaoId: String,
        legislaturaId: String,
        fonte: ComissaoConteudo,
    ): List<ComissaoMembro>

    /**
     * Every committee seat in the term, by person, re-emitting as the rows land.
     *
     * A flow because the thirty committees are downloaded one by one behind whoever is already
     * looking at the list: as a one-shot read the search would show the seats of whatever had
     * finished by the time the screen opened and then never change.
     */
    fun observeComissoesDosDeputados(legislaturaId: String): Flow<List<SelectComissoesDosDeputados>>

    suspend fun saveMembros(
        orgaoId: String,
        legislaturaId: String,
        fonte: ComissaoConteudo,
        membros: List<ComissaoMembro>,
        fetchedAt: Long,
    )
}
