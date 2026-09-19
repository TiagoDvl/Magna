package com.tick.magna.data.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AutoriaTest {

    @Test
    fun `one deputado is the ordinary case`() {
        // 190 of 197 propositions in a measured window look exactly like this.
        val autoria = autoriaDe("Daniel Trzeciak", "Deputado(a)", 1)

        assertEquals(Autoria("Daniel Trzeciak", TipoAutor.DEPUTADO, outros = 0), autoria)
    }

    @Test
    fun `a PEC carries its 171 other signatures as a number`() {
        val autoria = autoriaDe("Marcelo Freixo", "Deputado(a)", 172)

        assertEquals(171, autoria?.outros)
    }

    @Test
    fun `an orgao is an orgao, whichever one it is`() {
        listOf(
            "Órgão do Poder Executivo",
            "Órgão do Poder Legislativo",
            "COMISSÃO PERMANENTE",
        ).forEach { tipo ->
            assertEquals(TipoAutor.ORGAO, autoriaDe("Comissão de Finanças", tipo, 1)?.tipo, tipo)
        }
    }

    @Test
    fun `an unknown tipo is drawn as an institution rather than as a person`() {
        // The failure modes are not symmetric: a face on a comissao is wrong, an institution
        // icon on a deputado is only plain.
        assertEquals(TipoAutor.ORGAO, autoriaDe("Algo novo", "Tipo que nao existe ainda", 1)?.tipo)
        assertEquals(TipoAutor.ORGAO, autoriaDe("Algo novo", null, 1)?.tipo)
    }

    @Test
    fun `deputada is still a deputado`() {
        assertEquals(TipoAutor.DEPUTADO, autoriaDe("Tabata Amaral", "Deputada", 1)?.tipo)
    }

    @Test
    fun `no name means nothing to draw`() {
        assertNull(autoriaDe(null, "Deputado(a)", 1))
        assertNull(autoriaDe("   ", "Deputado(a)", 1))
    }

    @Test
    fun `a missing or impossible total never becomes a negative count`() {
        assertEquals(0, autoriaDe("Fulano", "Deputado(a)", null)?.outros)
        assertEquals(0, autoriaDe("Fulano", "Deputado(a)", 0)?.outros)
    }

    @Test
    fun `a proposition is named by its number, and by its sigla when it has none`() {
        val comNumero = Proposicao(
            id = "1", type = "PL", ementa = "", dataApresentacao = "",
            numero = 1589, ano = 2026,
        )
        val semNumero = comNumero.copy(numero = null)

        assertEquals("PL 1589/2026", comNumero.identificacao)
        assertEquals("PL", semNumero.identificacao)
    }
}
