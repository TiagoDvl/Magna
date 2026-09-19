package com.tick.magna.data.source.local.dao

import com.tick.magna.Proposicao
import kotlinx.coroutines.flow.Flow

interface ProposicaoDaoInterface {

    fun insertProposicoes(proposicoes: List<Proposicao>)

    /** The siglas of one bucket, for the screen that filters by kind of instrument. */
    fun getProposicoesNosTipos(
        legislaturaId: String,
        tipos: List<String>,
        limite: Long,
    ): Flow<List<Proposicao>>

    /**
     * Everything the given siglas do not cover, which is how Tramitacao is asked for.
     *
     * It has no list of its own: 544 siglas exist and the app will meet ones written after
     * this code, so the only definition that stays true is the complement of the three
     * closed buckets.
     */
    fun getProposicoesForaDosTipos(
        legislaturaId: String,
        tipos: List<String>,
        limite: Long,
    ): Flow<List<Proposicao>>

    /** Every type mixed, which is what the Home shows. */
    fun getProposicoes(legislaturaId: String, limite: Long): Flow<List<Proposicao>>
}
