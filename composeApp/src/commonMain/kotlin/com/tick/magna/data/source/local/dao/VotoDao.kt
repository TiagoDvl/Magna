package com.tick.magna.data.source.local.dao

import com.tick.magna.MagnaDatabase
import com.tick.magna.SelectVotosDaVotacao
import com.tick.magna.SelectVotosDoDeputado
import com.tick.magna.Voto
import com.tick.magna.VotacaoNominal
import com.tick.magna.VotoQueries
import com.tick.magna.VotoSync
import com.tick.magna.VotoSyncQueries

class VotoDao(
    private val database: MagnaDatabase,
    private val votoQueries: VotoQueries,
    private val votoSyncQueries: VotoSyncQueries,
) : VotoDaoInterface {

    override suspend fun getVotosDoDeputado(
        legislaturaId: String,
        deputadoId: String,
    ): List<SelectVotosDoDeputado> {
        return votoQueries.selectVotosDoDeputado(legislaturaId, deputadoId).executeAsList()
    }

    override suspend fun getVotacao(legislaturaId: String, votacaoId: String): VotacaoNominal? {
        return votoQueries.selectVotacaoNominal(votacaoId, legislaturaId).executeAsOneOrNull()
    }

    override suspend fun getVotosDaVotacao(
        legislaturaId: String,
        votacaoId: String,
    ): List<SelectVotosDaVotacao> {
        return votoQueries.selectVotosDaVotacao(legislaturaId, votacaoId).executeAsList()
    }

    override suspend fun getSync(legislaturaId: String): VotoSync? {
        return votoSyncQueries.selectVotoSync(legislaturaId).executeAsOneOrNull()
    }

    override suspend fun getVotacoesSincronizadas(legislaturaId: String): Set<String> {
        return votoQueries.selectVotacoesNominaisSincronizadas(legislaturaId).executeAsList().toSet()
    }

    override suspend fun saveVotos(
        votacoes: List<VotacaoNominal>,
        votos: List<Voto>,
        sync: VotoSync,
    ) {
        // One transaction rather than one per row. A single quarter of plenary votes is close
        // to six thousand rows, and each of those as its own transaction is a disk sync each.
        database.transaction {
            votacoes.forEach { votoQueries.insertVotacaoNominal(it) }
            votos.forEach { votoQueries.insertVoto(it) }
            votoSyncQueries.insertVotoSync(
                sync.legislaturaId,
                sync.windowStart,
                sync.windowEnd,
                sync.fetchedAt,
            )
        }
    }
}
