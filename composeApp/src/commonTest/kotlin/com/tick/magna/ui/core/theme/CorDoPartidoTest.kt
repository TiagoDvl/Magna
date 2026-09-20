package com.tick.magna.ui.core.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The colour that comes off a logo is a fact about a GIF, and the question here is whether it
 * can carry text once the theme is done with it.
 *
 * The hues are the real ones: the reds, blues, greens and yellows party marks are printed in.
 * A fixed lightness band passed a test like this for blue and failed it for yellow, which is
 * why the adjustment measures contrast instead of assuming it from lightness.
 */
class CorDoPartidoTest {

    private val branco = Color(0xFFFFFFFF)
    private val superficieClara = Color(0xFFF5F3ED)
    private val superficieEscura = Color(0xFF212427)

    @Test
    fun `every hue clears the small-text bar on a light surface`() {
        semTons().forEach { cor ->
            listOf(branco, superficieClara).forEach { fundo ->
                val ajustada = cor.comContraste(fundo, MINIMO)

                assertTrue(
                    contraste(ajustada, fundo) >= MINIMO - FOLGA,
                    "$cor on $fundo came out at ${contraste(ajustada, fundo)}",
                )
            }
        }
    }

    @Test
    fun `every hue clears it on a dark surface too`() {
        semTons().forEach { cor ->
            val ajustada = cor.comContraste(superficieEscura, MINIMO)

            assertTrue(
                contraste(ajustada, superficieEscura) >= MINIMO - FOLGA,
                "$cor came out at ${contraste(ajustada, superficieEscura)}",
            )
        }
    }

    @Test
    fun `yellow is the case a lightness band gets wrong`() {
        // Pure yellow at HSL lightness 0.40 measures 1.72:1 against white while blue at the
        // same lightness measures 8.7:1. A band would have passed this colour through.
        val amarelo = Color(0xFFFFE000)

        assertTrue(contraste(amarelo, branco) < 2f, "the premise: yellow is invisible on white")
        assertTrue(contraste(amarelo.comContraste(branco, MINIMO), branco) >= MINIMO - FOLGA)
    }

    @Test
    fun `a colour that is already legible is returned untouched`() {
        val azulEscuro = Color(0xFF16326B)

        assertEquals(azulEscuro, azulEscuro.comContraste(branco, MINIMO))
    }

    @Test
    fun `the hue survives the adjustment`() {
        // The whole reason for moving lightness rather than blending toward the text colour:
        // a red has to come out a red, or two parties end up the same grey.
        val vermelho = Color(0xFFFF4444)
        val ajustada = vermelho.comContraste(branco, MINIMO)

        assertTrue(ajustada.red > ajustada.green, "still warm")
        assertTrue(ajustada.red > ajustada.blue, "still red")
        assertTrue(abs(ajustada.green - ajustada.blue) < 0.1f, "and not drifting to orange")
    }

    @Test
    fun `grey has no hue to keep and is moved anyway`() {
        val cinza = Color(0xFF9A9A9A)
        val ajustada = cinza.comContraste(branco, MINIMO)

        assertTrue(contraste(ajustada, branco) >= MINIMO - FOLGA)
        assertEquals(ajustada.red, ajustada.green)
        assertEquals(ajustada.green, ajustada.blue)
    }

    /** A hue every thirty degrees, at the saturations marks are actually printed in. */
    private fun semTons(): List<Color> = buildList {
        for (grau in 0 until 360 step 30) {
            listOf(0.45f, 0.75f, 1f).forEach { saturacao ->
                listOf(0.35f, 0.5f, 0.65f).forEach { claridade ->
                    add(deHsl(grau.toFloat(), saturacao, claridade))
                }
            }
        }
    }

    private fun deHsl(matiz: Float, saturacao: Float, claridade: Float): Color {
        val c = (1f - abs(2f * claridade - 1f)) * saturacao
        val x = c * (1f - abs((matiz / 60f) % 2f - 1f))
        val m = claridade - c / 2f

        val (r, g, b) = when {
            matiz < 60f -> Triple(c, x, 0f)
            matiz < 120f -> Triple(x, c, 0f)
            matiz < 180f -> Triple(0f, c, x)
            matiz < 240f -> Triple(0f, x, c)
            matiz < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        return Color(r + m, g + m, b + m)
    }

    private companion object {
        const val MINIMO = 4.5f

        /** Twelve halvings do not land exactly on the bar; this is the width of the last step. */
        const val FOLGA = 0.05f
    }
}
