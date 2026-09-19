package com.tick.magna.data.source.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.tick.magna.MagnaDatabase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Proves 12.sqm runs over a Proposicao table shaped the way version 12 shaped it.
 *
 * The plugin's own `verifyMigrations` skips on this machine, so the eight new columns would
 * otherwise reach a device unproven — and a failed ALTER on a shipped install is not a blank
 * card, it is a database that will not open.
 *
 * The schema below is a frozen copy of the past. It is not meant to follow `Proposicao.sq`:
 * it is meant not to.
 */
class ProposicaoColunasTest {

    private lateinit var driver: JdbcSqliteDriver

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        VERSION_12_SCHEMA.forEach { statement -> driver.execute(null, statement, 0) }
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun the_cached_propositions_survive_the_new_columns() {
        driver.execute(null, "INSERT INTO Legislatura VALUES ('57', '2023-02-01', '2027-01-31')", 0)
        driver.execute(
            null,
            "INSERT INTO Proposicao VALUES " +
                "('2646830', '57', 'PL', 'Institui o Passaporte Equestre', " +
                "'2026-09-18T18:09', '204409', 'https://camara.leg.br/teor')",
            0,
        )

        MagnaDatabase.Schema.migrate(driver, oldVersion = 12, newVersion = 13).value

        val proposicoes = MagnaDatabase(driver).proposicaoQueries
            .getProposicoes("57", limite = 10)
            .executeAsList()

        assertEquals(1, proposicoes.size)

        val proposicao = proposicoes.single()
        assertEquals("Institui o Passaporte Equestre", proposicao.ementa)
        assertEquals("204409", proposicao.autores)

        // Null, not zero and not empty string: nobody asked the API for these when the row was
        // written, and the card has to be able to tell "no number" from "number zero".
        assertNull(proposicao.numero)
        assertNull(proposicao.ano)
        assertNull(proposicao.autorNome)
        assertNull(proposicao.autorTipo)
        assertNull(proposicao.autoresTotal)
        assertNull(proposicao.situacao)
        assertNull(proposicao.orgaoSigla)
        assertNull(proposicao.temas)
    }

    @Test
    fun a_row_written_after_the_migration_keeps_every_new_column() {
        driver.execute(null, "INSERT INTO Legislatura VALUES ('57', '2023-02-01', '2027-01-31')", 0)
        MagnaDatabase.Schema.migrate(driver, oldVersion = 12, newVersion = 13).value

        val database = MagnaDatabase(driver)
        database.proposicaoQueries.insertProposicao(
            com.tick.magna.Proposicao(
                id = "2646831",
                legislaturaId = "57",
                codTipo = "PEC",
                ementa = "Altera o Ato das Disposições Constitucionais Transitórias",
                dataApresentacao = "2026-09-18T18:09",
                autores = "178957",
                url = null,
                numero = 87,
                ano = 2025,
                autorNome = "Marcelo Freixo",
                autorTipo = "Deputado(a)",
                autoresTotal = 172,
                situacao = "Pronta para Pauta",
                orgaoSigla = "CCJC",
                temas = "Administração Pública | Direito e Justiça",
            )
        )

        val proposicao = database.proposicaoQueries
            .getProposicoes("57", limite = 10)
            .executeAsList()
            .single()

        assertEquals(87L, proposicao.numero)
        assertEquals(172L, proposicao.autoresTotal)
        assertEquals("Marcelo Freixo", proposicao.autorNome)
        assertEquals("Administração Pública | Direito e Justiça", proposicao.temas)
    }

    private companion object {
        val VERSION_12_SCHEMA = listOf(
            """
            CREATE TABLE Legislatura (
                id TEXT NOT NULL PRIMARY KEY,
                startDate TEXT NOT NULL,
                endDate TEXT NOT NULL
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
        )
    }
}
