package com.tick.magna.data.source.local.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.tick.magna.DeputadoDetails as DeputadoDetailsEntity

class DeputadoDetailsMapperTest {

    private fun entity(socials: String? = null) = DeputadoDetailsEntity(
        deputadoId = "204528",
        legislaturaId = "57",
        gabineteBuilding = "4",
        gabineteRoom = "101",
        gabineteTelephone = "2367004",
        gabineteEmail = "dep@camara.leg.br",
        urlWebsite = "https://deputado.com.br",
        socials = socials,
    )

    @Test
    fun toDomain_classifies_known_networks_by_url() {
        val socials = entity(
            socials = "https://facebook.com/dep, https://instagram.com/dep, https://youtube.com/@dep"
        ).toDomain().socials

        assertEquals("https://facebook.com/dep", socials["Facebook"])
        assertEquals("https://instagram.com/dep", socials["Instagram"])
        assertEquals("https://youtube.com/@dep", socials["Youtube"])
    }

    @Test
    fun toDomain_returns_empty_socials_when_column_is_null() {
        assertTrue(entity(socials = null).toDomain().socials.isEmpty())
    }

    @Test
    fun toDomain_drops_unrecognized_networks() {
        // Documents the gap in docs/review-v1.1.md section 4.6: classification is a
        // contains() over four hardcoded names, so x.com and any new network vanish.
        val socials = entity(socials = "https://x.com/dep, https://tiktok.com/@dep").toDomain().socials

        assertTrue(socials.isEmpty())
    }

    @Test
    fun toDomain_keeps_last_url_when_a_network_repeats() {
        val socials = entity(
            socials = "https://facebook.com/first, https://facebook.com/second"
        ).toDomain().socials

        assertEquals(1, socials.size)
        assertEquals("https://facebook.com/second", socials["Facebook"])
    }

    @Test
    fun toDomain_passes_gabinete_fields_through() {
        val details = entity().toDomain()

        assertEquals("4", details.gabineteBuilding)
        assertEquals("101", details.gabineteRoom)
        assertEquals("2367004", details.gabineteTelephone)
        assertEquals("dep@camara.leg.br", details.gabineteEmail)
        assertEquals("https://deputado.com.br", details.urlWebsite)
    }

    @Test
    fun toDomain_keeps_null_gabinete_fields_null() {
        val details = DeputadoDetailsEntity(
            deputadoId = "204528",
            legislaturaId = "57",
            gabineteBuilding = null,
            gabineteRoom = null,
            gabineteTelephone = null,
            gabineteEmail = null,
            urlWebsite = null,
            socials = null,
        ).toDomain()

        assertNull(details.gabineteBuilding)
        assertNull(details.gabineteRoom)
        assertNull(details.gabineteTelephone)
        assertNull(details.gabineteEmail)
        assertNull(details.urlWebsite)
    }
}
