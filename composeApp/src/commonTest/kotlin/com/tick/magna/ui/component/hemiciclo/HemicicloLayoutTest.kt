package com.tick.magna.ui.component.hemiciclo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HemicicloLayoutTest {

    /** The 57th legislature, as the register had it when this was written. */
    private val camara = listOf(
        Bancada("PL", 98), Bancada("PT", 65), Bancada("UNIÃO", 52), Bancada("PSD", 48),
        Bancada("PP", 46), Bancada("REPUBLICANOS", 42), Bancada("MDB", 38), Bancada("PODE", 27),
        Bancada("PSDB", 17), Bancada("PSB", 17), Bancada("PSOL", 13), Bancada("PCdoB", 11),
        Bancada("PDT", 9), Bancada("PV", 6), Bancada("NOVO", 5), Bancada("AVANTE", 5),
        Bancada("SOLIDARIEDADE", 4), Bancada("REDE", 3), Bancada("PRD", 3),
        Bancada("CIDADANIA", 2), Bancada("DC", 1), Bancada("MISSÃO", 1),
    )

    @Test
    fun `every seat of the house is drawn`() {
        // The count is the whole point of a seat chart. Proportional rounding over twelve rows
        // lands two or three short unless the remainder is handed back out.
        assertEquals(513, assentosPorFileira(513, fileiras = 12, raioInterno = 0.45f).sum())
        assertEquals(513, hemicicloLayout(camara, largura = 1000f, altura = 500f).posicoes.size)
    }

    @Test
    fun `outer rows hold more seats than inner ones`() {
        // Capacity follows radius, which is what keeps the dots evenly spaced instead of
        // crowded on the inside.
        val fileiras = assentosPorFileira(513, fileiras = 12, raioInterno = 0.45f)

        assertEquals(fileiras, fileiras.sortedBy { it })
        assertTrue(fileiras.last() > fileiras.first())
    }

    @Test
    fun `a house of nothing draws nothing`() {
        assertEquals(emptyList(), assentosPorFileira(0, fileiras = 12, raioInterno = 0.45f))
        assertTrue(hemicicloLayout(camara, largura = 0f, altura = 0f).posicoes.isEmpty())
        assertTrue(hemicicloLayout(emptyList(), largura = 100f, altura = 50f).posicoes.isEmpty())
    }

    @Test
    fun `a bench is one contiguous slice`() {
        // This is what lets a highlight be a subList instead of a filter, and what makes a
        // party read as a block rather than as scattered dots.
        val layout = hemicicloLayout(camara, largura = 1000f, altura = 500f)

        assertEquals(camara.size + 1, layout.inicios.size)
        assertEquals(0, layout.inicios.first())
        assertEquals(513, layout.inicios.last())

        camara.forEachIndexed { index, bancada ->
            assertEquals(
                bancada.assentos,
                layout.inicios[index + 1] - layout.inicios[index],
                bancada.sigla,
            )
        }
    }

    @Test
    fun `seats walk the arc from one end to the other`() {
        // Ordered by angle, so consecutive seats are neighbours. Without it the benches would
        // interleave across rows and the chart would read as noise.
        val layout = hemicicloLayout(camara, largura = 1000f, altura = 500f)
        val xs = layout.posicoes.map { it.x }

        assertTrue(xs.first() < 500f, "the first seat is on the left")
        assertTrue(xs.last() > 500f, "the last seat is on the right")
    }

    @Test
    fun `seats at the near end are drawn bigger than seats at the far wall`() {
        val layout = hemicicloLayout(camara, largura = 1000f, altura = 500f)

        val naPonta = escalaDaFaixa(layout.faixas.first())
        val noMeio = escalaDaFaixa(layout.faixas[layout.faixas.size / 2])

        assertTrue(naPonta > noMeio, "the near end is drawn bigger")
        assertTrue(layout.faixas.all { it in 0 until FAIXAS_DE_PROFUNDIDADE })
    }

    @Test
    fun `the taper is small enough to read as distance rather than as shading`() {
        // The whole reason this came back after being cut: at a wide range the small dots
        // render washed out and the arc looks lit rather than deep.
        val maior = escalaDaFaixa(FAIXAS_DE_PROFUNDIDADE - 1)
        val menor = escalaDaFaixa(0)

        assertTrue(maior <= 1f, "a seat is never drawn past its own spacing")
        assertTrue(maior / menor < 1.5f, "the near end is bigger, not a different size class")
    }

    @Test
    fun `depth bands cover the range and stay inside it`() {
        // Depth is one at the far wall and zero at the two ends nearest the observer.
        assertEquals(0, faixaDeProfundidade(1f))
        assertEquals(FAIXAS_DE_PROFUNDIDADE - 1, faixaDeProfundidade(0f))
        // Out of range on either side clamps rather than indexing past the array.
        assertEquals(0, faixaDeProfundidade(9f))
        assertEquals(FAIXAS_DE_PROFUNDIDADE - 1, faixaDeProfundidade(-1f))

        (0 until FAIXAS_DE_PROFUNDIDADE).forEach { faixa ->
            assertTrue(escalaDaFaixa(faixa) in ESCALA_FUNDO..1f)
        }
    }

    @Test
    fun `every seat lands inside the box`() {
        val layout = hemicicloLayout(camara, largura = 800f, altura = 300f)

        assertTrue(layout.posicoes.all { it.x in 0f..800f })
        assertTrue(layout.posicoes.all { it.y in 0f..300f })
        assertTrue(layout.raioDoAssento > 0f)
    }

    @Test
    fun `a seat knows which bench it is in`() {
        val layout = hemicicloLayout(camara, largura = 1000f, altura = 500f)

        assertEquals(0, bancadaDoAssento(layout.inicios, 0))
        assertEquals(0, bancadaDoAssento(layout.inicios, 97))
        assertEquals(1, bancadaDoAssento(layout.inicios, 98))
        assertEquals(camara.lastIndex, bancadaDoAssento(layout.inicios, 512))
    }

    @Test
    fun `a seat that does not exist belongs to nobody`() {
        val layout = hemicicloLayout(camara, largura = 1000f, altura = 500f)

        assertEquals(-1, bancadaDoAssento(layout.inicios, -1))
        assertEquals(-1, bancadaDoAssento(layout.inicios, 513))
        assertEquals(-1, bancadaDoAssento(intArrayOf(0), 0))
    }
}
