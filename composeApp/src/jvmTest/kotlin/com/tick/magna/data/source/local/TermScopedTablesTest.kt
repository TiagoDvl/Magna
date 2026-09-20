package com.tick.magna.data.source.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.tick.magna.Deputado
import com.tick.magna.Legislatura
import com.tick.magna.MagnaDatabase
import com.tick.magna.Partido
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Runs against a real SQLite database rather than a fake, because the bug these cover was in
 * the schema and no fake would have reproduced it.
 *
 * Half the Camara is re-elected — of the 648 deputados of the 57th legislature, 332 also served
 * in the 56th. Keyed by id alone, those 332 were one row, and the upsert handed it to whichever
 * term was synced last: switching to the 56th emptied them out of the 57th, and coming back
 * found a term missing half its people with nothing able to notice, because a shorter list is
 * not an empty one.
 *
 * These sit in jvmTest rather than commonTest because that is where a JDBC driver exists.
 */
class TermScopedTablesTest {

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
    fun a_deputado_who_served_twice_belongs_to_both_terms() {
        database.deputadoQueries.insertDeputado(deputado("220593", "57", partido = "PL"))
        database.deputadoQueries.insertDeputado(deputado("220593", "56", partido = "PSL"))

        assertEquals(1, deputadosOf("57").size)
        assertEquals(1, deputadosOf("56").size)

        // And each term keeps its own party, which the shared row could not.
        assertEquals("PL", deputadosOf("57").single().partido)
        assertEquals("PSL", deputadosOf("56").single().partido)
    }

    @Test
    fun syncing_one_term_does_not_empty_the_other() {
        // What a term switch does: the whole roster of the new term is written.
        listOf("1", "2", "3").forEach { database.deputadoQueries.insertDeputado(deputado(it, "57")) }
        listOf("2", "3", "4").forEach { database.deputadoQueries.insertDeputado(deputado(it, "56")) }

        assertEquals(listOf("1", "2", "3"), deputadosOf("57").map { it.id }.sorted())
        assertEquals(listOf("2", "3", "4"), deputadosOf("56").map { it.id }.sorted())
    }

    @Test
    fun a_party_exists_in_every_term_it_ran_in() {
        database.partidoQueries.insertPartido("36844", "57", null, "PT", "Partido dos Trabalhadores", null, null, null, null, null, null, null, null, null, null)
        database.partidoQueries.insertPartido("36844", "56", null, "PT", "Partido dos Trabalhadores", null, null, null, null, null, null, null, null, null, null)

        assertEquals(1, database.partidoQueries.getPartidos("57").executeAsList().size)
        assertEquals(1, database.partidoQueries.getPartidos("56").executeAsList().size)
    }

    @Test
    fun an_office_belongs_to_the_mandate_it_was_looked_up_in() {
        database.deputadoDetailsQueries.insertDeputadoDetails(
            com.tick.magna.DeputadoDetails("220593", "57", "Anexo IV", "512", null, null, null, null)
        )
        database.deputadoDetailsQueries.insertDeputadoDetails(
            com.tick.magna.DeputadoDetails("220593", "56", "Anexo III", "201", null, null, null, null)
        )

        assertEquals("512", database.deputadoDetailsQueries.getDeputadoDetails("220593", "57").executeAsOne().gabineteRoom)
        assertEquals("201", database.deputadoDetailsQueries.getDeputadoDetails("220593", "56").executeAsOne().gabineteRoom)
    }

    @Test
    fun having_opened_someone_outlives_the_cache_being_rebuilt() {
        database.deputadoQueries.insertDeputado(deputado("220593", "57"))
        database.deputadoLastSeenQueries.upsertLastSeen("220593", 1_000L)

        // A re-sync rewrites the roster; the record of having opened them is not part of it.
        database.deputadoQueries.insertDeputado(deputado("220593", "57", name = "Novo Nome"))

        assertEquals(
            listOf("220593"),
            database.deputadoQueries.getDeputadosOrderedByLastSeen("57").executeAsList().map { it.id },
        )
    }

    @Test
    fun recently_opened_is_read_per_term_so_nobody_is_listed_twice() {
        database.deputadoQueries.insertDeputado(deputado("220593", "57"))
        database.deputadoQueries.insertDeputado(deputado("220593", "56"))
        database.deputadoLastSeenQueries.upsertLastSeen("220593", 1_000L)

        // One row per term joined against one row of last seen: without the term filter this
        // would return the same person once per mandate they served.
        assertEquals(1, database.deputadoQueries.getDeputadosOrderedByLastSeen("57").executeAsList().size)
        assertEquals(1, database.deputadoQueries.getDeputadosOrderedByLastSeen("56").executeAsList().size)
    }

    @Test
    fun the_most_recently_opened_comes_first() {
        listOf("1", "2", "3").forEach { database.deputadoQueries.insertDeputado(deputado(it, "57")) }
        database.deputadoLastSeenQueries.upsertLastSeen("1", 100L)
        database.deputadoLastSeenQueries.upsertLastSeen("2", 300L)
        database.deputadoLastSeenQueries.upsertLastSeen("3", 200L)

        assertEquals(
            listOf("2", "3", "1"),
            database.deputadoQueries.getDeputadosOrderedByLastSeen("57").executeAsList().map { it.id },
        )
    }

    private fun deputadosOf(legislaturaId: String) =
        database.deputadoQueries.getDeputados(legislaturaId).executeAsList()

    private fun deputado(
        id: String,
        legislaturaId: String,
        partido: String? = null,
        name: String = "Deputado $id",
    ) = Deputado(
        id = id,
        legislaturaId = legislaturaId,
        partido = partido,
        name = name,
        uf = "SP",
        profile_picture = null,
        email = null,
        emExercicio = null,
    )
}
