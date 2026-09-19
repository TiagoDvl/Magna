package com.tick.magna.data.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class ProposicaoBucketTest {

    @Test
    fun `PEC is the only thing that reaches the Constitution`() {
        assertEquals(ProposicaoBucket.CONSTITUICAO, proposicaoBucket("PEC"))
    }

    @Test
    fun `the four instruments that carry the force of law land together`() {
        listOf("PL", "PLP", "MPV", "PLV").forEach { sigla ->
            assertEquals(ProposicaoBucket.LEI, proposicaoBucket(sigla), sigla)
        }
    }

    @Test
    fun `a decree suspended by Congress is an act of Congress, not a law`() {
        listOf("PDL", "PDC", "PDS", "PRC").forEach { sigla ->
            assertEquals(ProposicaoBucket.ATO_LEGISLATIVO, proposicaoBucket(sigla), sigla)
        }
    }

    @Test
    fun `the types that actually fill the list are procedure`() {
        // Measured over one 90-day window: PRL 448, REQ 271, DOC 219, RIC 217, PAR 215.
        listOf("PRL", "REQ", "DOC", "RIC", "PAR", "EMC", "SBT", "MSC", "INC", "RDF")
            .forEach { sigla ->
                assertEquals(ProposicaoBucket.TRAMITACAO, proposicaoBucket(sigla), sigla)
            }
    }

    @Test
    fun `suffixed variants fall through instead of becoming a bucket of their own`() {
        listOf("SBT-A", "EMC-A", "PARF", "PRLP").forEach { sigla ->
            assertEquals(ProposicaoBucket.TRAMITACAO, proposicaoBucket(sigla), sigla)
        }
    }

    @Test
    fun `an unknown sigla is procedure rather than a crash or an empty badge`() {
        // The reference table holds 544 values and the app will meet siglas this list has
        // never seen. None of them may leave the badge without an icon.
        assertEquals(ProposicaoBucket.TRAMITACAO, proposicaoBucket("XYZ"))
        assertEquals(ProposicaoBucket.TRAMITACAO, proposicaoBucket(""))
    }

    @Test
    fun `case and padding do not change the answer`() {
        assertEquals(ProposicaoBucket.CONSTITUICAO, proposicaoBucket(" pec "))
        assertEquals(ProposicaoBucket.LEI, proposicaoBucket("plp"))
    }
}
