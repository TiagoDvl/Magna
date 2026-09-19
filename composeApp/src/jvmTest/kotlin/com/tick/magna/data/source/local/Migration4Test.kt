package com.tick.magna.data.source.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.tick.magna.MagnaDatabase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Runs `4.sqm` against a database built the way version 4 built it.
 *
 * The plugin's own `verifyMigrations` cannot run on Windows — it opens its SQLite connection
 * inside a forked worker whose temp directory it gets wrong — and nothing runs on push, so the
 * migrations that shipped so far were only ever proven by installing over a real device. This
 * is the cheapest thing that proves the statements execute and that the one piece of user data
 * involved survives them.
 *
 * The version 4 schema below is a frozen copy of the past, which is what the `.db` snapshots
 * would have been. It is not meant to follow the `.sq` files: it is meant not to.
 */
class Migration4Test {

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

        MagnaDatabase.Schema.migrate(driver, oldVersion = 4, newVersion = 5).value

        val database = MagnaDatabase(driver)

        // The roster is gone on purpose: every row was attributed to whichever term wrote it
        // last, which is exactly the information that could not be trusted.
        assertEquals(0, database.deputadoQueries.getDeputados("57").executeAsList().size)

        // What is kept is the one thing the Camara did not produce. Once the sync writes the
        // roster back, the deputado who had been opened is still on the recent list and the
        // one who never was is not.
        listOf("220593", "204521").forEach { id ->
            database.deputadoQueries.insertDeputado(
                com.tick.magna.Deputado(id, "57", "PL", "Deputado $id", "MT", null, null)
            )
        }

        assertEquals(
            listOf("220593"),
            database.deputadoQueries.getDeputadosOrderedByLastSeen("57").executeAsList().map { it.id },
        )
    }

    @Test
    fun the_migrated_database_keys_deputados_by_term() {
        driver.execute(null, "INSERT INTO Legislatura VALUES ('57', '2023-02-01', '2027-01-31')", 0)
        driver.execute(null, "INSERT INTO Legislatura VALUES ('56', '2019-02-01', '2023-01-31')", 0)

        MagnaDatabase.Schema.migrate(driver, oldVersion = 4, newVersion = 5).value

        val database = MagnaDatabase(driver)
        database.deputadoQueries.insertDeputado(
            com.tick.magna.Deputado("220593", "57", "PL", "Abilio", "MT", null, null)
        )
        database.deputadoQueries.insertDeputado(
            com.tick.magna.Deputado("220593", "56", "PSL", "Abilio", "MT", null, null)
        )

        assertEquals(1, database.deputadoQueries.getDeputados("57").executeAsList().size)
        assertEquals(1, database.deputadoQueries.getDeputados("56").executeAsList().size)
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
