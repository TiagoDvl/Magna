package com.tick.magna.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StringUtilsTest {

    @Test
    fun normalizeForSearch_lowercases() {
        assertEquals("marcelo freixo", "Marcelo Freixo".normalizeForSearch())
    }

    @Test
    fun normalizeForSearch_strips_portuguese_diacritics() {
        assertEquals("samia bomfim", "Sâmia Bomfim".normalizeForSearch())
        assertEquals("jose", "José".normalizeForSearch())
        assertEquals("uniao", "União".normalizeForSearch())
        assertEquals("goncalves", "Gonçalves".normalizeForSearch())
        assertEquals("ianio", "Iânio".normalizeForSearch())
    }

    @Test
    fun normalizeForSearch_covers_every_vowel_family() {
        assertEquals("aaaaa", "áàâãä".normalizeForSearch())
        assertEquals("eeee", "éèêë".normalizeForSearch())
        assertEquals("iiii", "íìîï".normalizeForSearch())
        assertEquals("ooooo", "óòôõö".normalizeForSearch())
        assertEquals("uuuu", "úùûü".normalizeForSearch())
        assertEquals("cn", "çñ".normalizeForSearch())
    }

    @Test
    fun normalizeForSearch_strips_diacritics_from_uppercase_input() {
        // lowercase() runs first, so uppercase accents must normalize too.
        assertEquals("samia", "SÂMIA".normalizeForSearch())
        assertEquals("uniao", "UNIÃO".normalizeForSearch())
    }

    @Test
    fun normalizeForSearch_makes_unaccented_query_match_accented_name() {
        val name = "Sâmia Bomfim".normalizeForSearch()

        assertTrue(name.contains("samia".normalizeForSearch()))
        assertTrue(name.contains("Samia".normalizeForSearch()))
    }

    @Test
    fun normalizeForSearch_leaves_plain_text_untouched() {
        assertEquals("", "".normalizeForSearch())
        assertEquals("tabata amaral", "tabata amaral".normalizeForSearch())
    }
}
