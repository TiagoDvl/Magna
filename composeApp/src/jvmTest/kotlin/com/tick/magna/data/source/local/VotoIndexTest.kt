package com.tick.magna.data.source.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.tick.magna.MagnaDatabase
import com.tick.magna.Voto
import com.tick.magna.VotacaoNominal
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The index that answers a question the API cannot be asked.
 *
 * `/deputados/{id}/votos` is an HTTP 405. The only endpoint is `/votacoes/{id}/votos`, which
 * answers the opposite question, so the sync goes votacao by votacao and this table turns it
 * around.
 */
class VotoIndexTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: MagnaDatabase

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        MagnaDatabase.Schema.create(driver)
        database = MagnaDatabase(driver)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun a_deputado_gets_their_own_votes_newest_first() {
        givenVotacao("v1", "2026-03-01T10:00")
        givenVotacao("v2", "2026-09-01T10:00")
        givenVoto("v1", "204501", "Sim")
        givenVoto("v2", "204501", "Não")
        givenVoto("v1", "999999", "Sim")

        val votos = database.votoQueries.selectVotosDoDeputado("57", "204501").executeAsList()

        assertEquals(listOf("v2", "v1"), votos.map { it.id })
        assertEquals(listOf("Não", "Sim"), votos.map { it.voto })
    }

    @Test
    fun the_votacao_comes_along_so_a_vote_is_not_a_bare_word() {
        givenVotacao(
            "v1",
            "2026-09-01T10:00",
            descricao = "Aprovado o Requerimento de Urgência. Sim: 320; Não: 41.",
            siglaOrgao = "PLEN",
        )
        givenVoto("v1", "204501", "Sim")

        val voto = database.votoQueries.selectVotosDoDeputado("57", "204501").executeAsOne()

        assertEquals("Aprovado o Requerimento de Urgência. Sim: 320; Não: 41.", voto.descricao)
        assertEquals("PLEN", voto.siglaOrgao)
        assertEquals(1L, voto.aprovacao)
    }

    @Test
    fun somebody_who_served_two_terms_does_not_mix_them() {
        givenVotacao("v1", "2026-09-01T10:00", legislaturaId = "57")
        givenVotacao("v9", "2022-09-01T10:00", legislaturaId = "56")
        givenVoto("v1", "204501", "Sim", legislaturaId = "57")
        givenVoto("v9", "204501", "Não", legislaturaId = "56")

        assertEquals(
            listOf("v1"),
            database.votoQueries.selectVotosDoDeputado("57", "204501").executeAsList().map { it.id },
        )
        assertEquals(
            listOf("v9"),
            database.votoQueries.selectVotosDoDeputado("56", "204501").executeAsList().map { it.id },
        )
    }

    @Test
    fun re_syncing_the_same_votacao_does_not_double_the_votes() {
        givenVotacao("v1", "2026-09-01T10:00")
        givenVoto("v1", "204501", "Sim")
        givenVoto("v1", "204501", "Não")

        // A window is swept again every six hours, and the votacoes inside it do not change.
        val votos = database.votoQueries.selectVotosDoDeputado("57", "204501").executeAsList()
        assertEquals(1, votos.size)
        assertEquals("Não", votos.single().voto)
    }

    @Test
    fun the_stored_votacoes_are_what_a_re_sweep_skips() {
        givenVotacao("v1", "2026-09-01T10:00")
        givenVotacao("v2", "2026-08-01T10:00")

        // Each one costs a request that returns about four hundred rows, so a refresh that
        // asked for them again would be the expensive part of an otherwise cheap sweep.
        assertEquals(
            setOf("v1", "v2"),
            database.votoQueries.selectVotacoesNominaisSincronizadas("57").executeAsList().toSet(),
        )
    }

    @Test
    fun a_term_that_was_never_swept_has_no_stamp_rather_than_an_empty_one() {
        // Eleven nominal votacoes is a normal quarter and zero is a plausible one, so "no
        // rows" cannot be the signal to re-download.
        assertNull(database.votoSyncQueries.selectVotoSync("57").executeAsOneOrNull())

        database.votoSyncQueries.insertVotoSync("57", "2026-06-19", "2026-09-19", 1_789_000_000_000L)

        val sync = database.votoSyncQueries.selectVotoSync("57").executeAsOne()
        assertEquals("2026-06-19", sync.windowStart)
        assertEquals(1_789_000_000_000L, sync.fetchedAt)
    }

    @Test
    fun a_deputado_with_no_votes_reads_as_empty_and_not_as_missing() {
        givenVotacao("v1", "2026-09-01T10:00")
        givenVoto("v1", "204501", "Sim")

        assertEquals(0, database.votoQueries.selectVotosDoDeputado("57", "111111").executeAsList().size)
    }

    private fun givenVotacao(
        id: String,
        dataHoraRegistro: String,
        legislaturaId: String = "57",
        descricao: String = "Aprovado. Sim: 10; Não: 2.",
        siglaOrgao: String = "PLEN",
    ) {
        database.votoQueries.insertVotacaoNominal(
            VotacaoNominal(
                id = id,
                legislaturaId = legislaturaId,
                dataHoraRegistro = dataHoraRegistro,
                descricao = descricao,
                siglaOrgao = siglaOrgao,
                aprovacao = 1L,
            )
        )
    }

    private fun givenVoto(
        votacaoId: String,
        deputadoId: String,
        voto: String,
        legislaturaId: String = "57",
    ) {
        database.votoQueries.insertVoto(
            Voto(
                votacaoId = votacaoId,
                deputadoId = deputadoId,
                legislaturaId = legislaturaId,
                voto = voto,
                dataHoraVoto = "2026-09-01T10:00",
            )
        )
    }
}
