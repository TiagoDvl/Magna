package com.tick.magna.data.source.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.tick.magna.MagnaDatabase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The order the committee list comes back in, against a real database.
 *
 * It replaced six ids written into an enum. That curation was a real product decision — the
 * names people recognise, and the busiest ones at the time — but it froze in 2023: measured
 * across the whole 57th legislature, the CCTI it names is twenty-ninth of thirty, while the
 * CPD and the CE, which it never named, are fifth and sixth.
 */
class ComissaoOrderingTest {

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
    fun the_busiest_committee_comes_first() {
        givenComissao("2003", "CCJC", atividade = 1922 to "57")
        givenComissao("6066", "CTUR", atividade = 91 to "57")
        givenComissao("5503", "CSPCCO", atividade = 742 to "57")

        assertEquals(listOf("CCJC", "CSPCCO", "CTUR"), siglasOf("57"))
    }

    @Test
    fun activity_is_read_per_term() {
        givenComissao("2002", "CCTI", atividade = 143 to "57")
        givenComissao("2014", "CSAUDE", atividade = 627 to "57")
        database.orgaoAtividadeQueries.insertOrgaoAtividade("2002", "56", 1507L)
        database.orgaoAtividadeQueries.insertOrgaoAtividade("2014", "56", 1536L)

        // The CCTI was third in the 56th and is near the bottom of the 57th. One stored number
        // would have frozen one of the two.
        assertEquals(listOf("CSAUDE", "CCTI"), siglasOf("57"))
        assertEquals(listOf("CSAUDE", "CCTI"), siglasOf("56"))
        assertEquals(1507L, atividadeOf("CCTI", "56"))
        assertEquals(143L, atividadeOf("CCTI", "57"))
    }

    @Test
    fun an_unmeasured_term_is_alphabetical_rather_than_arbitrary() {
        givenComissao("6066", "CTUR")
        givenComissao("2003", "CCJC")
        givenComissao("5503", "CSPCCO")

        // Insertion order would have been CTUR, CCJC, CSPCCO. Before any measurement arrives
        // the list has no ranking to offer, and says so by being alphabetical.
        assertEquals(listOf("CCJC", "CSPCCO", "CTUR"), siglasOf("57"))
    }

    @Test
    fun a_committee_that_was_not_measured_sorts_below_every_one_that_was() {
        givenComissao("2003", "CCJC", atividade = 1922 to "57")
        givenComissao("2001", "CAPADR")

        // Not measured is not the same as measured zero, and only one of them is a claim about
        // the committee. The unmeasured one goes last rather than to the top by alphabet.
        assertEquals(listOf("CCJC", "CAPADR"), siglasOf("57"))
        assertEquals(-1L, atividadeOf("CAPADR", "57"))
    }

    @Test
    fun a_committee_measured_at_zero_still_outranks_one_never_measured() {
        givenComissao("539388", "CASP", atividade = 0 to "56")
        givenComissao("2001", "CAPADR")

        assertEquals(listOf("CASP", "CAPADR"), siglasOf("56"))
    }

    @Test
    fun a_measurement_replaces_the_previous_one_instead_of_duplicating_the_row() {
        givenComissao("2003", "CCJC", atividade = 100 to "57")
        database.orgaoAtividadeQueries.insertOrgaoAtividade("2003", "57", 1922L)

        assertEquals(1, siglasOf("57").size)
        assertEquals(1922L, atividadeOf("CCJC", "57"))
    }

    @Test
    fun counting_what_is_missing_is_per_term_too() {
        givenComissao("2003", "CCJC", atividade = 1922 to "57")
        givenComissao("6066", "CTUR", atividade = 91 to "57")

        assertEquals(0L, database.orgaoAtividadeQueries.countOrgaosWithoutAtividade("57").executeAsOne())
        assertEquals(2L, database.orgaoAtividadeQueries.countOrgaosWithoutAtividade("56").executeAsOne())
    }

    private fun siglasOf(legislaturaId: String) =
        database.orgaoAtividadeQueries.selectOrgaosByAtividade(legislaturaId).executeAsList().map { it.sigla }

    private fun atividadeOf(sigla: String, legislaturaId: String) =
        database.orgaoAtividadeQueries.selectOrgaosByAtividade(legislaturaId).executeAsList()
            .single { it.sigla == sigla }
            .votacoes

    private fun givenComissao(id: String, sigla: String, atividade: Pair<Int, String>? = null) {
        database.orgaoQueries.insertOrgao(id, sigla, "Comissao $sigla", sigla)

        atividade?.let { (votacoes, legislaturaId) ->
            database.orgaoAtividadeQueries.insertOrgaoAtividade(id, legislaturaId, votacoes.toLong())
        }
    }
}
