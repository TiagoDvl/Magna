package com.tick.magna.data.repository.deputados

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DataDeReferenciaTest {

    private val quinquagesimaSetima = "2023-02-01" to "2027-01-31"
    private val quinquagesimaSexta = "2019-02-01" to "2023-01-31"

    @Test
    fun `the current term asks about today`() {
        val data = dataDeReferencia(
            startDate = quinquagesimaSetima.first,
            endDate = quinquagesimaSetima.second,
            today = "2026-09-20",
        )

        assertEquals("2026-09-20", data)
    }

    @Test
    fun `a finished term asks about its last day`() {
        // Measured: 2023-01-31 returns the 510 the 56th ended with. Its first day returns
        // nothing at all, which is why the answer is never the start date.
        val data = dataDeReferencia(
            startDate = quinquagesimaSexta.first,
            endDate = quinquagesimaSexta.second,
            today = "2026-09-20",
        )

        assertEquals("2023-01-31", data)
    }

    @Test
    fun `the first and last days of the current term are inside it`() {
        assertEquals("2023-02-01", dataDeReferencia("2023-02-01", "2027-01-31", "2023-02-01"))
        assertEquals("2027-01-31", dataDeReferencia("2023-02-01", "2027-01-31", "2027-01-31"))
    }

    @Test
    fun `a term that has not started has no reference date`() {
        // Answering with its end date would be answering about the future, and the endpoint
        // returns an empty list for a date it has not reached.
        assertNull(dataDeReferencia("2027-02-01", "2031-01-31", "2026-09-20"))
    }

    @Test
    fun `a date that is not a date leaves the marker unmeasured`() {
        assertNull(dataDeReferencia("", "2027-01-31", "2026-09-20"))
        assertNull(dataDeReferencia("2023-02-01", "nao sei", "2026-09-20"))
    }

    @Test
    fun `a stored timestamp is cut back to its day`() {
        val data = dataDeReferencia("2023-02-01T00:00", "2027-01-31T23:59", "2026-09-20T10:15")

        assertEquals("2026-09-20", data)
    }
}
