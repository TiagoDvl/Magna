package com.tick.magna.data.source.local.dao

import com.tick.magna.ComissaoMembro
import com.tick.magna.ComissaoVotacao
import com.tick.magna.ComissaoVotacaoProposicao

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

    suspend fun saveMembros(
        orgaoId: String,
        legislaturaId: String,
        fonte: ComissaoConteudo,
        membros: List<ComissaoMembro>,
        fetchedAt: Long,
    )
}
