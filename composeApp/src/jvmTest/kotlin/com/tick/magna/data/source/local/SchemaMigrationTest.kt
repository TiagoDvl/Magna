package com.tick.magna.data.source.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.tick.magna.MagnaDatabase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Runs the migrations against a database built the way version 4 built it.
 *
 * The plugin's own `verifyMigrations` cannot run on Windows — it opens its SQLite connection
 * inside a forked worker whose temp directory it gets wrong — and nothing runs on push, so the
 * migrations that shipped so far were only ever proven by installing over a real device. This
 * is the cheapest thing that proves the statements execute and that the one piece of user data
 * involved survives them.
 *
 * The version 4 schema below is a frozen copy of the past, which is what the `.db` snapshots
 * would have been. It is not meant to follow the `.sq` files: it is meant not to.
 *
 * Every case migrates all the way to the current version rather than to the one migration it
 * is about. Stopping partway used to pass, and then stopped compiling the moment a later
 * migration added a column the generated queries read — which is the same thing a device does
 * when it upgrades across several releases at once.
 */
class SchemaMigrationTest {

    private lateinit var driver: JdbcSqliteDriver

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        VERSION_4_SCHEMA.forEach { statement -> driver.execute(null, statement, 0) }
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun the_migration_runs_and_keeps_what_the_person_produced() {
        driver.execute(null, "INSERT INTO Legislatura VALUES ('57', '2023-02-01', '2027-01-31')", 0)
        driver.execute(
            null,
            "INSERT INTO Deputado VALUES ('220593', '57', 'PL', 1700000000000, 'Abilio', 'MT', NULL, NULL)",
            0,
        )
        driver.execute(
            null,
            "INSERT INTO Deputado VALUES ('204521', '57', 'PT', 0, 'Nunca Aberto', 'SP', NULL, NULL)",
            0,
        )

        MagnaDatabase.Schema.migrate(driver, oldVersion = 4L, newVersion = MagnaDatabase.Schema.version).value

        val database = MagnaDatabase(driver)

        // The roster is gone on purpose: every row was attributed to whichever term wrote it
        // last, which is exactly the information that could not be trusted.
        assertEquals(0, database.deputadoQueries.getDeputados("57").executeAsList().size)

        // What is kept is the one thing the Camara did not produce. Once the sync writes the
        // roster back, the deputado who had been opened is still on the recent list and the
        // one who never was is not.
        listOf("220593", "204521").forEach { id ->
            database.deputadoQueries.insertDeputado(
                com.tick.magna.Deputado(id, "57", "PL", "Deputado $id", "MT", null, null, null)
            )
        }

        assertEquals(
            listOf("220593"),
            database.deputadoQueries.getDeputadosOrderedByLastSeen("57").executeAsList().map { it.id },
        )
    }

    @Test
    fun the_whole_chain_runs_for_somebody_upgrading_from_the_shipped_release() {
        driver.execute(null, "INSERT INTO Legislatura VALUES ('57', '2023-02-01', '2027-01-31')", 0)

        // Version 4 is what is in the store today, so this is the jump a real update makes.
        MagnaDatabase.Schema.migrate(driver, oldVersion = 4L, newVersion = MagnaDatabase.Schema.version).value

        val database = MagnaDatabase(driver)
        database.deputadoBioQueries.insertDeputadoBio(
            com.tick.magna.DeputadoBio("220593", "M", "1984-01-31", "MT", "Cuiaba")
        )

        assertEquals(
            listOf("MT"),
            database.deputadoBioQueries.getDeputadoBios(listOf("220593")).executeAsList()
                .map { it.ufNascimento },
        )
    }

    @Test
    fun a_biography_is_not_keyed_by_term_because_it_does_not_change() {
        MagnaDatabase.Schema.migrate(driver, oldVersion = 4L, newVersion = MagnaDatabase.Schema.version).value
        val database = MagnaDatabase(driver)

        database.deputadoBioQueries.insertDeputadoBio(
            com.tick.magna.DeputadoBio("220593", "M", "1984-01-31", "MT", "Cuiaba")
        )
        database.deputadoBioQueries.insertDeputadoBio(
            com.tick.magna.DeputadoBio("220593", "M", "1984-01-31", "MT", "Cuiaba")
        )

        // One row per person, however many mandates they served.
        assertEquals(1, database.deputadoBioQueries.getDeputadoBios(listOf("220593")).executeAsList().size)
    }

    @Test
    fun the_migrated_database_keys_deputados_by_term() {
        driver.execute(null, "INSERT INTO Legislatura VALUES ('57', '2023-02-01', '2027-01-31')", 0)
        driver.execute(null, "INSERT INTO Legislatura VALUES ('56', '2019-02-01', '2023-01-31')", 0)

        MagnaDatabase.Schema.migrate(driver, oldVersion = 4L, newVersion = MagnaDatabase.Schema.version).value

        val database = MagnaDatabase(driver)
        database.deputadoQueries.insertDeputado(
            com.tick.magna.Deputado("220593", "57", "PL", "Abilio", "MT", null, null, null)
        )
        database.deputadoQueries.insertDeputado(
            com.tick.magna.Deputado("220593", "56", "PSL", "Abilio", "MT", null, null, null)
        )

        assertEquals(1, database.deputadoQueries.getDeputados("57").executeAsList().size)
        assertEquals(1, database.deputadoQueries.getDeputados("56").executeAsList().size)
    }

    @Test
    fun the_committee_cache_lands_empty_on_an_upgrade_rather_than_missing() {
        // Migration 8 adds four tables and copies nothing, because none of this data existed
        // anywhere before: the committee screens went from Ktor straight to the UI. What has
        // to be true after an upgrade is that the tables are there and answer.
        // 8.sqm takes the schema from 8 to 9, so the whole chain from the shipped release is
        // 4 to 9. Getting this off by one is how the test found the migration had not run.
        MagnaDatabase.Schema.migrate(driver, oldVersion = 4L, newVersion = MagnaDatabase.Schema.version).value
        val database = MagnaDatabase(driver)

        assertEquals(
            0,
            database.comissaoVotacaoQueries.selectComissaoVotacoes("2003", "57").executeAsList().size,
        )
        assertEquals(
            0,
            database.comissaoMembroQueries.selectComissaoComposicao("2003", "57").executeAsList().size,
        )

        // Null rather than zero: nothing was ever downloaded, which is not the same as having
        // downloaded nothing. That is the whole reason ComissaoCache exists.
        assertNull(
            database.comissaoCacheQueries.selectComissaoCache("2003", "57", "VOTACOES")
                .executeAsOneOrNull()
        )
    }

    private companion object {
        val VERSION_4_SCHEMA = listOf(
            """
            CREATE TABLE Legislatura (
                id TEXT NOT NULL PRIMARY KEY,
                startDate TEXT NOT NULL,
                endDate TEXT NOT NULL
            )
            """.trimIndent(),
            """
            CREATE TABLE Deputado (
                id TEXT NOT NULL PRIMARY KEY,
                legislaturaId TEXT NOT NULL,
                partido TEXT,
                last_seen INTEGER NOT NULL DEFAULT 0,
                name TEXT,
                uf TEXT,
                profile_picture TEXT,
                email TEXT,
                FOREIGN KEY(legislaturaId) REFERENCES Legislatura(id)
            )
            """.trimIndent(),
            """
            CREATE TABLE Partido (
                id TEXT NOT NULL PRIMARY KEY,
                legislaturaId TEXT NOT NULL,
                liderDeputadoId TEXT,
                sigla TEXT NOT NULL,
                nome TEXT NOT NULL,
                situacao TEXT,
                totalPosse TEXT,
                totalMembros TEXT,
                logo TEXT,
                website TEXT,
                FOREIGN KEY(liderDeputadoId) REFERENCES Deputado(id),
                FOREIGN KEY(legislaturaId) REFERENCES Legislatura(id)
            )
            """.trimIndent(),
            """
            CREATE TABLE Proposicao (
                id TEXT NOT NULL,
                legislaturaId TEXT NOT NULL,
                codTipo TEXT,
                ementa TEXT,
                dataApresentacao TEXT,
                autores TEXT,
                url TEXT,
                PRIMARY KEY (id, legislaturaId),
                FOREIGN KEY(legislaturaId) REFERENCES Legislatura(id)
            )
            """.trimIndent(),
            """
            CREATE TABLE DeputadoDetails(
                deputadoId TEXT NOT NULL PRIMARY KEY,
                legislaturaId TEXT NOT NULL,
                gabineteBuilding TEXT,
                gabineteRoom TEXT,
                gabineteTelephone TEXT,
                gabineteEmail TEXT,
                urlWebsite TEXT,
                socials TEXT,
                FOREIGN KEY(deputadoId) REFERENCES Deputado(id),
                FOREIGN KEY(legislaturaId) REFERENCES Legislatura(id)
            )
            """.trimIndent(),
        )
    }
}
