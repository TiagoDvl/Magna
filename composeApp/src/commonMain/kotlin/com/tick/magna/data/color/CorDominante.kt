package com.tick.magna.data.color

import kotlin.math.max
import kotlin.math.min

/**
 * The colour a logo is mostly made of, or null when it is not made of any.
 *
 * **Most present, not most striking.** The count decides, because a party's identity is the
 * colour that covers its mark rather than the one that catches the eye in it: the PSDB's logo
 * carries yellow and blue, and the answer should be whichever of them the logo is actually
 * made of.
 *
 * **What is thrown away first is the whole trick.** A party logo is a mark on a white field,
 * usually with black lettering, and counting every pixel answers "white" for all of them. So
 * anything transparent, anything too dark, too light, or too close to grey is dropped before
 * anything is counted. What survives is the chromatic part of the image, which is the part
 * that means something.
 *
 * Null when nothing survives — a mark that is genuinely black and white has no colour to find,
 * and inventing one for it would be worse than saying so.
 *
 * @param pixels ARGB, one int per pixel, in any order. Sampling them is the caller's business.
 */
internal fun corDominante(pixels: IntArray): Int? {
    val somaVermelho = IntArray(BALDES)
    val somaVerde = IntArray(BALDES)
    val somaAzul = IntArray(BALDES)
    val contagem = IntArray(BALDES)

    pixels.forEach { pixel ->
        val alfa = (pixel ushr 24) and 0xFF
        if (alfa < ALFA_MINIMO) return@forEach

        val vermelho = (pixel ushr 16) and 0xFF
        val verde = (pixel ushr 8) and 0xFF
        val azul = pixel and 0xFF

        val maior = max(vermelho, max(verde, azul))
        val menor = min(vermelho, min(verde, azul))

        if (maior < ESCURO_DEMAIS || menor > CLARO_DEMAIS) return@forEach
        if (maior == 0) return@forEach

        val saturacao = (maior - menor).toFloat() / maior
        if (saturacao < SATURACAO_MINIMA) return@forEach

        val balde = balde(vermelho, verde, azul)

        somaVermelho[balde] += vermelho
        somaVerde[balde] += verde
        somaAzul[balde] += azul
        contagem[balde]++
    }

    var escolhido = -1
    var maiorContagem = 0
    contagem.forEachIndexed { balde, quantos ->
        if (quantos > maiorContagem) {
            maiorContagem = quantos
            escolhido = balde
        }
    }

    if (escolhido < 0) return null

    // The average of the bucket rather than its centre: the bucket is coarse on purpose, so
    // that two shades of the same red count as one colour, and then the answer is the colour
    // those pixels actually were.
    val vermelho = somaVermelho[escolhido] / maiorContagem
    val verde = somaVerde[escolhido] / maiorContagem
    val azul = somaAzul[escolhido] / maiorContagem

    return (0xFF shl 24) or (vermelho shl 16) or (verde shl 8) or azul
}

/**
 * Which of [BALDES] boxes a colour falls in, three bits per channel.
 *
 * Coarse deliberately. A logo is compressed and antialiased, so a flat red arrives as a few
 * hundred nearly-identical reds; counted exactly, each one is a colour with a count of two and
 * the winner is noise. Eight levels per channel puts them all in one box.
 */
private fun balde(vermelho: Int, verde: Int, azul: Int): Int =
    ((vermelho shr 5) shl 6) or ((verde shr 5) shl 3) or (azul shr 5)

private const val BALDES = 8 * 8 * 8

/** Below this a pixel is see-through, and a logo's field usually is. */
private const val ALFA_MINIMO = 128

/** Lettering and outlines. Dark enough and the hue underneath means nothing. */
private const val ESCURO_DEMAIS = 40

/** The white the mark sits on, which is most of the image in every logo measured. */
private const val CLARO_DEMAIS = 235

/** Under this a colour is a grey with an opinion, and greys are not identities. */
private const val SATURACAO_MINIMA = 0.30f
