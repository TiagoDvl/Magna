package com.tick.magna.data.repository.proposicoes

import com.tick.magna.data.domain.ProposicaoBucket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BucketContagensTest {

    private val medido = mapOf(
        ProposicaoBucket.CONSTITUICAO to 1,
        ProposicaoBucket.LEI to 2161,
        ProposicaoBucket.ATO_LEGISLATIVO to 323,
    )

    @Test
    fun `procedure is what is left of the window`() {
        // The numbers of one measured 90-day window of the 57th.
        val contagens = contagensPorBucket(total = 11333, fechados = medido)

        assertEquals(11333, contagens[null])
        assertEquals(1, contagens[ProposicaoBucket.CONSTITUICAO])
        assertEquals(2161, contagens[ProposicaoBucket.LEI])
        assertEquals(323, contagens[ProposicaoBucket.ATO_LEGISLATIVO])
        assertEquals(8848, contagens[ProposicaoBucket.TRAMITACAO])
    }

    @Test
    fun `every chip adds up to the total`() {
        val contagens = contagensPorBucket(total = 11333, fechados = medido)
        val soma = ProposicaoBucket.entries.sumOf { contagens.getValue(it) }

        assertEquals(contagens.getValue(null), soma)
    }

    @Test
    fun `a missing bucket leaves procedure without a number rather than a wrong one`() {
        // With LEI unknown the subtraction would read 11009 and be believed. The chip shows
        // its name and nothing else instead.
        val contagens = contagensPorBucket(
            total = 11333,
            fechados = medido + (ProposicaoBucket.LEI to null),
        )

        assertNull(contagens[ProposicaoBucket.TRAMITACAO])
        assertEquals(1, contagens[ProposicaoBucket.CONSTITUICAO])
    }

    @Test
    fun `a missing total leaves only the buckets that answered`() {
        val contagens = contagensPorBucket(total = null, fechados = medido)

        assertNull(contagens[null])
        assertNull(contagens[ProposicaoBucket.TRAMITACAO])
        assertEquals(2161, contagens[ProposicaoBucket.LEI])
    }

    @Test
    fun `nothing at all is an empty map, not zeros`() {
        val contagens = contagensPorBucket(total = null, fechados = emptyMap())

        assertTrue(contagens.isEmpty())
    }

    @Test
    fun `a window refiled between the two requests does not go negative`() {
        // The total and the buckets are separate requests; the Camara can file in between.
        val contagens = contagensPorBucket(total = 10, fechados = medido)

        assertEquals(0, contagens[ProposicaoBucket.TRAMITACAO])
    }
}
