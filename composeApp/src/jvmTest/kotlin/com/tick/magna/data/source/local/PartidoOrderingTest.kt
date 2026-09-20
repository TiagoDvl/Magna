package com.tick.magna.data.source.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.tick.magna.Deputado
import com.tick.magna.Legislatura
import com.tick.magna.MagnaDatabase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The ordering of the party list, against a real database because that is where it lives now.
 *
 * It used to live in two view models, both doing `sortedByDescending { totalMembros }` over a
 * column the sync never fills. Every value was null, so the sort was a no-op and what reached
 * the screen was insertion order — the order the API returns, which is alphabetical. The Home
 * carousel took the first eight of that, so it showed AVANTE and CIDADANIA where it meant to
 * show the largest parties.
 */
class PartidoOrderingTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: MagnaDatabase

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        MagnaDatabase.Schema.create(driver)
        database = MagnaDatabase(driver)

        database.legislaturaQueries.insertLegislatura(Legislatura("57", "2023-02-01", "2027-01-31"))
        database.legislaturaQueries.insertLegislatura(Legislatura("56", "2019-02-01", "2023-01-31"))
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun parties_are_ordered_by_how_many_deputados_they_have() {
        givenParty("1", "PL", deputados = 116)
        givenParty("2", "PT", deputados = 79)
        givenParty("3", "AVANTE", deputados = 8)

        // Alphabetically this would be AVANTE, PL, PT, which is exactly what used to show.
        assertEquals(listOf("PL", "PT", "AVANTE"), siglasOf("57"))
    }

    @Test
    fun the_chosen_order_wins_over_size() {
        givenParty("1", "PL", deputados = 116)
        givenParty("2", "PT", deputados = 79)
        givenParty("3", "NOVO", deputados = 4)

        // The whole order, not one row of it: that is what the screen writes.
        givenOrdem("3", "1", "2")

        assertEquals(listOf("NOVO", "PL", "PT"), siglasOf("57"))
        assertEquals(listOf(0L, 1L, 2L), rowsOf("57").map { it.posicao })
    }

    @Test
    fun a_party_without_a_position_falls_in_behind_the_chosen_ones() {
        // Not a state the screen writes, because it records every party at once. It is the
        // state a party joining the register lands in, and it has to sort somewhere sensible:
        // after everything chosen, and by size among its own kind.
        givenParty("1", "PL", deputados = 116)
        givenParty("2", "PT", deputados = 79)
        givenParty("3", "NOVO", deputados = 4)

        givenOrdem("3")

        assertEquals(listOf("NOVO", "PL", "PT"), siglasOf("57"))
        assertEquals(null, rowsOf("57").single { it.sigla == "PL" }.posicao)
    }

    @Test
    fun no_order_at_all_is_ordered_by_size() {
        givenParty("1", "PL", deputados = 116)
        givenParty("2", "PT", deputados = 79)
        givenParty("3", "AVANTE", deputados = 8)

        assertEquals(listOf("PL", "PT", "AVANTE"), siglasOf("57"))
        assertTrue(rowsOf("57").all { it.posicao == null })
    }

    @Test
    fun the_order_is_not_tied_to_a_term() {
        givenParty("1", "PL", deputados = 116, legislaturaId = "57")
        givenParty("1", "PL", deputados = 40, legislaturaId = "56")
        givenParty("2", "NOVO", deputados = 4, legislaturaId = "57")
        givenParty("2", "NOVO", deputados = 8, legislaturaId = "56")

        givenOrdem("2", "1")

        // Wanting the NOVO at the top is something you said, not a fact about a mandate.
        assertEquals(listOf("NOVO", "PL"), siglasOf("57"))
        assertEquals(listOf("NOVO", "PL"), siglasOf("56"))
    }

    /** Writes the whole order, which is the only way the app ever writes one. */
    private fun givenOrdem(vararg partidoIds: String) {
        database.partidoOrdemQueries.deletePartidoOrdem()
        partidoIds.forEachIndexed { posicao, id ->
            database.partidoOrdemQueries.insertPartidoOrdem(id, posicao.toLong())
        }
    }

    @Test
    fun the_count_is_per_term() {
        givenParty("1", "PL", deputados = 116, legislaturaId = "57")
        givenParty("1", "PL", deputados = 40, legislaturaId = "56")

        assertEquals(116, rowsOf("57").single().deputadoCount)
        assertEquals(40, rowsOf("56").single().deputadoCount)
    }

    @Test
    fun ties_fall_back_to_the_alphabet_instead_of_whatever_order_the_rows_were_written_in() {
        givenParty("1", "PSD", deputados = 10)
        givenParty("2", "MDB", deputados = 10)

        assertEquals(listOf("MDB", "PSD"), siglasOf("57"))
    }

    private fun siglasOf(legislaturaId: String) = rowsOf(legislaturaId).map { it.sigla }

    private fun rowsOf(legislaturaId: String) =
        database.partidoQueries.getPartidos(legislaturaId).executeAsList()

    /**
     * Writes the party and the deputados that make up its count, because the count is a
     * subquery over the Deputado table rather than a stored column — no endpoint gives a size
     * that works for an older term.
     */
    private fun givenParty(
        id: String,
        sigla: String,
        deputados: Int,
        legislaturaId: String = "57",
    ) {
        database.partidoQueries.insertPartido(
            id, legislaturaId, null, sigla, "Partido $sigla", null, null, null, null, null,
            null, null, null, null, null,
        )

        repeat(deputados) { index ->
            database.deputadoQueries.insertDeputado(
                Deputado(
                    id = "$sigla-$legislaturaId-$index",
                    legislaturaId = legislaturaId,
                    partido = sigla,
                    name = "Deputado $index",
                    uf = "SP",
                    profile_picture = null,
                    email = null,
                    emExercicio = null,
                )
            )
        }
    }
}
