package com.tick.magna.features.deputados.details

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GabineteTest {

    @Test
    fun `the building and the room become one address`() {
        val sala = salaDoGabinete("4", "420")

        assertEquals("4", sala?.anexo)
        assertEquals("420", sala?.sala)
    }

    @Test
    fun `a building that is not a number is dropped rather than printed`() {
        // One gabinete in the twenty measured has `x` for predio. "Anexo x" names nothing, so
        // the room still shows and the map link does not.
        val sala = salaDoGabinete("x", "420")

        assertEquals("420", sala?.sala)
        assertNull(sala?.anexo)
        assertNull(mapaDoGabinete(sala?.anexo))
    }

    @Test
    fun `no room means no address at all`() {
        // A building alone points at four hundred doors.
        assertNull(salaDoGabinete("4", null))
        assertNull(salaDoGabinete("4", "  "))
    }

    @Test
    fun `the map query names the annex and not the room`() {
        val url = mapaDoGabinete("4")

        assertTrue(url!!.startsWith("https://www.google.com/maps/search/?api=1&query="))
        assertTrue(url.contains("Anexo%204"))
        // Spaces and commas encoded, nothing left raw.
        assertTrue(!url.substringAfter("query=").contains(' '))
    }

    @Test
    fun `eight digits get the area code every gabinete shares`() {
        // The register sends `3215-5420` and eight digits dial nowhere.
        assertEquals("tel:+556132155420", telefoneDiscavel("3215-5420"))
        assertEquals("(61) 3215-5420", telefoneLegivel("3215-5420"))
    }

    @Test
    fun `a longer number is passed through rather than prefixed twice`() {
        assertEquals("tel:+556132155420", telefoneDiscavel("(61) 3215-5420"))
        assertEquals("tel:+5511998765432", telefoneDiscavel("11 99876-5432"))
        assertEquals("tel:+556132155420", telefoneDiscavel("+55 61 3215-5420"))
    }

    @Test
    fun `a number nothing can dial is offered as text and not as a link`() {
        assertNull(telefoneDiscavel(null))
        assertNull(telefoneDiscavel(""))
        assertNull(telefoneDiscavel("221"))
        assertEquals("221", telefoneLegivel("221"))
    }

    @Test
    fun `an address with no at sign is not a mailto`() {
        assertEquals("mailto:dep@camara.leg.br", emailDiscavel(" dep@camara.leg.br "))
        assertNull(emailDiscavel("sem arroba"))
        assertNull(emailDiscavel(null))
    }
}
