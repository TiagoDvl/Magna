package com.tick.magna.ui.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.tick.magna.data.domain.Partido
import com.tick.magna.data.domain.PartidoDetail
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * The colour a party is drawn in.
 *
 * One place, so every screen that names a party agrees about it.
 *
 * The party's own colour is read from its logo at sync time and stored raw. What arrives here
 * is therefore a fact about a GIF — the PT's red is whatever red the register's file is made
 * of — and a fact about a GIF is not automatically a colour you can put text in. So it is
 * moved, as little as possible, until it is: hue and saturation kept, lightness walked toward
 * the far end until it clears [CONTRASTE_MINIMO] against the surface underneath.
 *
 * That is the split worth keeping. The record says what the party's colour *is*; the theme
 * decides what it looks like here. A party whose logo the register does not host has no colour
 * to adapt and falls back to the area's own blue, which is what every party used before any of
 * them had one of their own.
 */
val Partido.corDeExibicao: Color
    @Composable
    @ReadOnlyComposable
    get() = corDeExibicao(cor)

val PartidoDetail.corDeExibicao: Color
    @Composable
    @ReadOnlyComposable
    get() = corDeExibicao(cor)

@Composable
@ReadOnlyComposable
private fun corDeExibicao(argb: Int?): Color {
    if (argb == null) return MagnaArea.PARTIDOS.accent

    return Color(argb).comContraste(MaterialTheme.colorScheme.surface, CONTRASTE_MINIMO)
}

/**
 * The same colour, moved just far enough to be readable on [fundo].
 *
 * **Why lightness and not a blend toward the text colour.** Blending drags every party toward
 * the same grey and two reds end up the same red. Moving lightness along the party's own hue
 * keeps a red a red — a yellow that has to clear 4.5:1 on white comes out as a dark gold,
 * which is the honest consequence of asking a yellow to carry small text.
 *
 * **Why a search and not a fixed band.** A fixed lightness band was the first attempt and it
 * is wrong, because HSL lightness is not luminance: yellow at lightness 0.40 measures 1.72:1
 * against white while blue at the same lightness measures 8.7:1. Contrast has to be measured,
 * not assumed from a number that only looks like brightness. The search returns the lightness
 * closest to the original that clears the bar, so a mark that was already legible is returned
 * untouched and the rest move the minimum distance.
 */
internal fun Color.comContraste(fundo: Color, minimo: Float): Color {
    if (contraste(this, fundo) >= minimo) return this

    // Which way to walk is decided by the surface, not by a flag: on a dark surface the only
    // direction that gains contrast is lighter, and on a light one it is darker.
    val clarear = fundo.luminance() < LIMITE_DE_SUPERFICIE
    val claridade = claridade()

    var baixo = if (clarear) claridade else 0f
    var alto = if (clarear) 1f else claridade

    repeat(PASSOS_DA_BUSCA) {
        val meio = (baixo + alto) / 2f

        if (contraste(comClaridade(meio), fundo) >= minimo) {
            // Clears it, so try closer to the colour the party actually is.
            if (clarear) alto = meio else baixo = meio
        } else {
            if (clarear) baixo = meio else alto = meio
        }
    }

    return comClaridade(if (clarear) alto else baixo)
}

/** WCAG relative-luminance contrast, the same ratio the rest of this palette was measured on. */
internal fun contraste(uma: Color, outra: Color): Float {
    val maior = max(uma.luminance(), outra.luminance())
    val menor = min(uma.luminance(), outra.luminance())

    return (maior + 0.05f) / (menor + 0.05f)
}

private fun Color.claridade(): Float =
    (max(red, max(green, blue)) + min(red, min(green, blue))) / 2f

/** The same hue and saturation at a different lightness. */
private fun Color.comClaridade(alvo: Float): Color {
    val maior = max(red, max(green, blue))
    val menor = min(red, min(green, blue))

    if (maior == menor) return Color(alvo, alvo, alvo, alpha)

    val claridade = (maior + menor) / 2f
    val amplitude = maior - menor
    val saturacao = if (claridade > 0.5f) {
        amplitude / (2f - maior - menor)
    } else {
        amplitude / (maior + menor)
    }

    val matiz = matiz(maior, amplitude)
    val c = (1f - abs(2f * alvo - 1f)) * saturacao
    val x = c * (1f - abs((matiz / 60f) % 2f - 1f))
    val m = alvo - c / 2f

    val (r, g, b) = when {
        matiz < 60f -> Triple(c, x, 0f)
        matiz < 120f -> Triple(x, c, 0f)
        matiz < 180f -> Triple(0f, c, x)
        matiz < 240f -> Triple(0f, x, c)
        matiz < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    return Color(
        red = (r + m).coerceIn(0f, 1f),
        green = (g + m).coerceIn(0f, 1f),
        blue = (b + m).coerceIn(0f, 1f),
        alpha = alpha,
    )
}

/** Degrees around the wheel, from whichever channel is on top. */
private fun Color.matiz(maior: Float, amplitude: Float): Float {
    if (amplitude == 0f) return 0f

    val bruto = when (maior) {
        red -> 60f * (((green - blue) / amplitude) % 6f)
        green -> 60f * ((blue - red) / amplitude + 2f)
        else -> 60f * ((red - green) / amplitude + 4f)
    }

    return if (bruto < 0f) bruto + 360f else bruto
}

/** Below this the surface is a dark one, whatever the theme calls itself. */
private const val LIMITE_DE_SUPERFICIE = 0.5f

/**
 * The small-text threshold, and it is the right one here rather than the 3.0 for graphics: the
 * party's colour lands on siglas and on labels, not only on shapes.
 */
private const val CONTRASTE_MINIMO = 4.5f

/** Twelve halvings of a unit range lands within a thousandth, which is under a colour step. */
private const val PASSOS_DA_BUSCA = 12
