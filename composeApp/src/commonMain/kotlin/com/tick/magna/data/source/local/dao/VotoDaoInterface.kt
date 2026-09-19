package com.tick.magna.data.source.local.dao

import com.tick.magna.SelectVotosDaVotacao
import com.tick.magna.SelectVotosDoDeputado
import com.tick.magna.Voto
import com.tick.magna.VotacaoNominal
import com.tick.magna.VotoSync

interface VotoDaoInterface {

    /**
     * The one read the whole table exists for, answered by the index rather than by a scan of
     * every vote of the term.
     */
    suspend fun getVotosDoDeputado(legislaturaId: String, deputadoId: String): List<SelectVotosDoDeputado>

    /** The votacao itself, or null when this term never swept the window it is in. */
    suspend fun getVotacao(legislaturaId: String, votacaoId: String): VotacaoNominal?

    /** Everybody who voted in it, with the person joined in from the roster. */
    suspend fun getVotosDaVotacao(legislaturaId: String, votacaoId: String): List<SelectVotosDaVotacao>

    /**
     * What window has been downloaded for this term, or null if none has. Null and an empty
     * result are different answers: eleven nominal votacoes is a normal quarter, and zero is
     * a plausible one.
     */
    suspend fun getSync(legislaturaId: String): VotoSync?

    /**
     * The votacoes already stored, so a re-sync does not ask for their votes again. Each one
     * costs a request and returns about four hundred rows.
     */
    suspend fun getVotacoesSincronizadas(legislaturaId: String): Set<String>

    /**
     * One transaction for the whole window: the votacoes, their votes and the stamp. Tens of
     * thousands of rows, so they do not go in one at a time.
     */
    suspend fun saveVotos(
        votacoes: List<VotacaoNominal>,
        votos: List<Voto>,
        sync: VotoSync,
    )
}
