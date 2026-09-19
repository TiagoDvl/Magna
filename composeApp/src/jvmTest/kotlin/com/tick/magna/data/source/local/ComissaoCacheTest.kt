package com.tick.magna.data.source.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.tick.magna.ComissaoMembro
import com.tick.magna.ComissaoVotacao
import com.tick.magna.ComissaoVotacaoProposicao
import com.tick.magna.MagnaDatabase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * What the committee cache stores and gives back, against a real database.
 *
 * The screen it serves had nothing: every tab went from Ktor to the UI and wrote nothing down,
 * so a committee already visited was a spinner and then an error without a connection.
 */
class ComissaoCacheTest {

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
    fun votes_come_back_newest_first() {
        givenVotacao("1", "2026-03-01T10:00")
        givenVotacao("2", "2026-09-01T10:00")
        givenVotacao("3", "2026-06-01T10:00")

        val stored = database.comissaoVotacaoQueries
            .selectComissaoVotacoes("2003", "57")
            .executeAsList()

        // Ordered on the raw ISO timestamp, which is why the column stores what the API sent
        // rather than the 01/03/2026 the card shows.
        assertEquals(listOf("2", "3", "1"), stored.map { it.votacaoId })
    }

    @Test
    fun the_propositions_of_a_vote_keep_the_order_the_api_listed_them_in() {
        givenVotacao("1", "2026-03-01T10:00")
        givenProposicao("1", "45678", ordem = 1, rotulo = "PL 2/2024")
        givenProposicao("1", "12345", ordem = 0, rotulo = "PL 1/2023")

        val stored = database.comissaoVotacaoQueries
            .selectComissaoVotacaoProposicoes("2003", "57")
            .executeAsList()

        // There is no other order to fall back on, and a card that reshuffles its own
        // propositions between visits looks broken.
        assertEquals(listOf("12345", "45678"), stored.map { it.proposicaoId })
    }

    @Test
    fun the_propositions_of_another_committee_do_not_come_along() {
        givenVotacao("1", "2026-03-01T10:00", orgaoId = "2003")
        givenProposicao("1", "12345", ordem = 0)
        givenVotacao("9", "2026-03-01T10:00", orgaoId = "2001")
        givenProposicao("9", "99999", ordem = 0)

        val stored = database.comissaoVotacaoQueries
            .selectComissaoVotacaoProposicoes("2003", "57")
            .executeAsList()

        // They are keyed by the vote alone, so the scoping has to come through the join.
        assertEquals(listOf("12345"), stored.map { it.proposicaoId })
    }

    @Test
    fun a_refresh_that_returns_less_does_not_leave_the_old_votes_behind() {
        givenVotacao("1", "2026-03-01T10:00")
        givenVotacao("2", "2026-09-01T10:00")
        givenProposicao("1", "12345", ordem = 0)

        database.comissaoVotacaoQueries.deleteComissaoVotacaoProposicoes("2003", "57")
        database.comissaoVotacaoQueries.deleteComissaoVotacoes("2003", "57")
        givenVotacao("2", "2026-09-01T10:00")

        assertEquals(
            listOf("2"),
            database.comissaoVotacaoQueries.selectComissaoVotacoes("2003", "57")
                .executeAsList().map { it.votacaoId },
        )
        assertEquals(
            0,
            database.comissaoVotacaoQueries.selectComissaoVotacaoProposicoes("2003", "57")
                .executeAsList().size,
        )
    }

    @Test
    fun the_same_term_of_another_committee_is_untouched() {
        givenVotacao("1", "2026-03-01T10:00", orgaoId = "2003")
        givenVotacao("9", "2026-03-01T10:00", orgaoId = "2001")

        database.comissaoVotacaoQueries.deleteComissaoVotacoes("2003", "57")

        assertEquals(
            listOf("9"),
            database.comissaoVotacaoQueries.selectComissaoVotacoes("2001", "57")
                .executeAsList().map { it.votacaoId },
        )
    }

    @Test
    fun a_president_fetched_for_the_timeline_is_not_a_member_of_the_current_composition() {
        // The two tabs come from the same endpoint asked two different ways. Without `fonte`
        // in the key, a president from 2023 would show up in the composition of today.
        givenMembro("178860", fonte = "COMPOSICAO", dataInicio = "2026-02-09", codTitulo = 1)
        givenMembro("141428", fonte = "PRESIDENCIA", dataInicio = "2023-03-15", codTitulo = 1)

        assertEquals(
            listOf("178860"),
            database.comissaoMembroQueries.selectComissaoComposicao("2003", "57")
                .executeAsList().map { it.deputadoId },
        )
        assertEquals(
            listOf("141428"),
            database.comissaoMembroQueries.selectComissaoPresidencia("2003", "57")
                .executeAsList().map { it.deputadoId },
        )
    }

    @Test
    fun a_composition_comes_back_in_the_order_the_screen_draws_it() {
        givenMembro("5", fonte = "COMPOSICAO", dataInicio = "2026-02-01", codTitulo = 102, nome = "Zeca")
        givenMembro("4", fonte = "COMPOSICAO", dataInicio = "2026-02-01", codTitulo = 101, nome = "Ana")
        givenMembro("1", fonte = "COMPOSICAO", dataInicio = "2026-02-09", codTitulo = 1, nome = "Leur")
        givenMembro("3", fonte = "COMPOSICAO", dataInicio = "2026-02-01", codTitulo = 101, nome = "Bia")

        // Mesa first, then titulares and suplentes, alphabetical inside each. Nothing stores
        // the order because it is reproducible.
        assertEquals(
            listOf("1", "4", "3", "5"),
            database.comissaoMembroQueries.selectComissaoComposicao("2003", "57")
                .executeAsList().map { it.deputadoId },
        )
    }

    @Test
    fun a_presidency_comes_back_most_recent_first() {
        givenMembro("141428", fonte = "PRESIDENCIA", dataInicio = "2023-03-15", codTitulo = 1)
        givenMembro("204436", fonte = "PRESIDENCIA", dataInicio = "2026-02-10", codTitulo = 1)
        givenMembro("220552", fonte = "PRESIDENCIA", dataInicio = "2024-03-06", codTitulo = 1)

        assertEquals(
            listOf("204436", "220552", "141428"),
            database.comissaoMembroQueries.selectComissaoPresidencia("2003", "57")
                .executeAsList().map { it.deputadoId },
        )
    }

    @Test
    fun somebody_who_presided_twice_in_one_term_is_two_rows() {
        givenMembro("141428", fonte = "PRESIDENCIA", dataInicio = "2023-03-15", codTitulo = 1)
        givenMembro("141428", fonte = "PRESIDENCIA", dataInicio = "2025-03-19", codTitulo = 1)

        // dataInicio is in the key for exactly this. The same person is not a repeat; the same
        // person over the same period is.
        assertEquals(
            2,
            database.comissaoMembroQueries.selectComissaoPresidencia("2003", "57")
                .executeAsList().size,
        )
    }

    @Test
    fun a_committee_that_was_asked_about_and_had_nothing_is_not_a_committee_never_asked_about() {
        // The CASP has no votes at all. Without the stamp, that emptiness would be downloaded
        // again on every single visit, forever.
        assertNull(
            database.comissaoCacheQueries.selectComissaoCache("5467", "57", "VOTACOES")
                .executeAsOneOrNull()
        )

        database.comissaoCacheQueries.insertComissaoCache("5467", "57", "VOTACOES", 1_789_000_000_000L)

        assertEquals(
            1_789_000_000_000L,
            database.comissaoCacheQueries.selectComissaoCache("5467", "57", "VOTACOES")
                .executeAsOne(),
        )
        assertEquals(
            0,
            database.comissaoVotacaoQueries.selectComissaoVotacoes("5467", "57").executeAsList().size,
        )
    }

    @Test
    fun each_piece_of_the_screen_is_stamped_on_its_own() {
        database.comissaoCacheQueries.insertComissaoCache("2003", "57", "VOTACOES", 100L)
        database.comissaoCacheQueries.insertComissaoCache("2003", "57", "COMPOSICAO", 200L)

        assertEquals(
            100L,
            database.comissaoCacheQueries.selectComissaoCache("2003", "57", "VOTACOES").executeAsOne(),
        )
        assertEquals(
            200L,
            database.comissaoCacheQueries.selectComissaoCache("2003", "57", "COMPOSICAO").executeAsOne(),
        )
        assertNull(
            database.comissaoCacheQueries.selectComissaoCache("2003", "57", "PRESIDENCIA")
                .executeAsOneOrNull()
        )
    }

    @Test
    fun the_same_committee_in_two_terms_is_two_caches() {
        givenVotacao("1", "2026-03-01T10:00", legislaturaId = "57")
        givenVotacao("9", "2022-03-01T10:00", legislaturaId = "56")

        assertEquals(
            listOf("1"),
            database.comissaoVotacaoQueries.selectComissaoVotacoes("2003", "57")
                .executeAsList().map { it.votacaoId },
        )
        assertEquals(
            listOf("9"),
            database.comissaoVotacaoQueries.selectComissaoVotacoes("2003", "56")
                .executeAsList().map { it.votacaoId },
        )
    }

    private fun givenVotacao(
        votacaoId: String,
        dataHoraRegistro: String?,
        orgaoId: String = "2003",
        legislaturaId: String = "57",
    ) {
        database.comissaoVotacaoQueries.insertComissaoVotacao(
            ComissaoVotacao(
                orgaoId = orgaoId,
                legislaturaId = legislaturaId,
                votacaoId = votacaoId,
                dataHoraRegistro = dataHoraRegistro,
                descricao = "Aprovado o Parecer.",
                aprovacao = 1L,
                parecer = null,
                idEvento = null,
            )
        )
    }

    private fun givenProposicao(
        votacaoId: String,
        proposicaoId: String,
        ordem: Int,
        rotulo: String? = null,
    ) {
        database.comissaoVotacaoQueries.insertComissaoVotacaoProposicao(
            ComissaoVotacaoProposicao(
                votacaoId = votacaoId,
                proposicaoId = proposicaoId,
                ordem = ordem.toLong(),
                rotulo = rotulo,
                ementa = "Ementa de $proposicaoId",
            )
        )
    }

    private fun givenMembro(
        deputadoId: String,
        fonte: String,
        dataInicio: String,
        codTitulo: Int,
        nome: String = "Deputado $deputadoId",
    ) {
        database.comissaoMembroQueries.insertComissaoMembro(
            ComissaoMembro(
                orgaoId = "2003",
                legislaturaId = "57",
                fonte = fonte,
                deputadoId = deputadoId,
                dataInicio = dataInicio,
                nome = nome,
                siglaPartido = "PT",
                siglaUf = "SP",
                urlFoto = null,
                titulo = if (codTitulo == 1) "Presidente" else "Titular",
                codTitulo = codTitulo.toLong(),
                dataFim = null,
            )
        )
    }
}
