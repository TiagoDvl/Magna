package com.tick.magna.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RedeSocialTest {

    @Test
    fun `the four hosts the register actually holds`() {
        // Measured over forty deputados: facebook 22, twitter 21, instagram 21, youtube 14.
        assertEquals(
            "instagram://user?username=adailfilho",
            appDeRedeSocial("https://www.instagram.com/adailfilho/"),
        )
        assertEquals(
            "twitter://user?screen_name=acaciofavacho",
            appDeRedeSocial("https://twitter.com/acaciofavacho"),
        )
        assertTrue(
            appDeRedeSocial("https://www.facebook.com/deputadofederalacaciofavacho")!!
                .startsWith("fb://facewebmodal/f?href=")
        )
        // YouTube claims its own https links and its scheme addresses videos, not channels.
        assertNull(appDeRedeSocial("https://youtube.com/channel/UCWNbptIjAriL2pt8H3W6CGw"))
    }

    @Test
    fun `x com is matched too even though the register has none`() {
        assertEquals("twitter://user?screen_name=alguem", appDeRedeSocial("https://x.com/alguem"))
    }

    @Test
    fun `a post is not a profile`() {
        // `?username=p` would open the app on a profile that does not exist.
        assertNull(appDeRedeSocial("https://www.instagram.com/p/Cxyz123/"))
        assertNull(appDeRedeSocial("https://twitter.com/i/status/1234"))
    }

    @Test
    fun `a bare host has no profile to open`() {
        assertNull(appDeRedeSocial("https://www.instagram.com/"))
        assertNull(appDeRedeSocial("https://facebook.com"))
    }

    @Test
    fun `a query string is not part of the name`() {
        assertEquals(
            "instagram://user?username=alguem",
            appDeRedeSocial("https://instagram.com/alguem?igshid=abc"),
        )
    }

    @Test
    fun `anything else keeps the web link`() {
        assertNull(appDeRedeSocial("https://www.deputado.com.br"))
        assertNull(appDeRedeSocial("https://linkedin.com/in/alguem"))
    }

    @Test
    fun `the facebook link is encoded rather than pasted raw`() {
        val url = appDeRedeSocial("https://www.facebook.com/dep tado")!!

        assertTrue(!url.substringAfter("href=").contains(' '))
    }
}
