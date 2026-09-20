package com.tick.magna.data.color

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A party logo is a mark on a white field, so the interesting cases are all about what is
 * thrown away before anything is counted.
 */
class CorDominanteTest {

    @Test
    fun `the white a mark sits on does not win`() {
        // Nine parts white to one part red, which is roughly what a party logo is.
        val pixels = IntArray(100) { if (it < 10) VERMELHO else BRANCO }

        val cor = corDominante(pixels)

        assertEquals(VERMELHO, cor)
    }

    @Test
    fun `lettering does not win either`() {
        val pixels = IntArray(100) {
            when {
                it < 10 -> VERMELHO
                it < 60 -> PRETO
                else -> BRANCO
            }
        }

        assertEquals(VERMELHO, corDominante(pixels))
    }

    @Test
    fun `a transparent field is not a colour`() {
        val pixels = IntArray(100) { if (it < 10) VERMELHO else TRANSPARENTE }

        assertEquals(VERMELHO, corDominante(pixels))
    }

    @Test
    fun `greys are not identities`() {
        val pixels = IntArray(100) { if (it < 10) VERMELHO else CINZA }

        assertEquals(VERMELHO, corDominante(pixels))
    }

    @Test
    fun `a mark with no colour in it has no colour`() {
        // The honest answer for a black and white logo. Inventing a hue for it would put a
        // party in a colour that nothing about the party says.
        assertNull(corDominante(IntArray(100) { if (it % 2 == 0) PRETO else BRANCO }))
        assertNull(corDominante(IntArray(0)))
    }

    @Test
    fun `the most present colour wins, not the most striking one`() {
        // Sixty parts a muted blue against ten of a screaming magenta.
        val pixels = IntArray(100) {
            when {
                it < 60 -> AZUL
                it < 70 -> MAGENTA
                else -> BRANCO
            }
        }

        assertEquals(AZUL, corDominante(pixels))
    }

    @Test
    fun `near-identical shades of one colour count as one colour`() {
        // What compression and antialiasing do to a flat fill. Counted exactly, every one of
        // these is a colour with a count of one and the winner is whichever came first.
        val pixels = IntArray(60) { argb(0xC0 + it % 6, 0x10 + it % 4, 0x12 + it % 3) } +
            IntArray(40) { AZUL }

        val cor = corDominante(pixels)!!

        val vermelho = (cor shr 16) and 0xFF
        val azul = cor and 0xFF
        assertTrue(vermelho > 0xB0, "the reds won: $vermelho")
        assertTrue(azul < 0x40, "and they are red, not blue: $azul")
    }

    @Test
    fun `the answer is opaque whatever the pixels were`() {
        val cor = corDominante(IntArray(10) { VERMELHO and 0x80FFFFFF.toInt() })

        assertEquals(0xFF, (cor!! ushr 24) and 0xFF)
    }

    private companion object {
        fun argb(r: Int, g: Int, b: Int) = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

        val VERMELHO = argb(0xCC, 0x11, 0x11)
        val AZUL = argb(0x22, 0x44, 0xAA)
        val MAGENTA = argb(0xFF, 0x00, 0xE0)
        val BRANCO = argb(0xFF, 0xFF, 0xFF)
        val PRETO = argb(0x0A, 0x0A, 0x0A)
        val CINZA = argb(0x88, 0x8A, 0x8C)
        const val TRANSPARENTE = 0x00CC1111
    }
}
