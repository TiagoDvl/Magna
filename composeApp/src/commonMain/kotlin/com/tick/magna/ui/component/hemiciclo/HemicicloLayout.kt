package com.tick.magna.ui.component.hemiciclo

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * One party's bench, in the order the chart draws them.
 *
 * @param assentos seats held on the term's reference date, not everyone who passed through it.
 * The two differ by a lot — 648 people have held one of the 513 seats of the 57th — and an arc
 * built from the larger number is not the house.
 */
data class Bancada(val sigla: String, val assentos: Int)

/**
 * Where every seat of the house goes, computed once for a given size and bench list.
 *
 * @param posicoes every seat, ordered so that consecutive seats are angularly adjacent. A
 * party therefore occupies one contiguous slice of this list and one wedge of the arc, which
 * is what lets a highlight be a range instead of a filter.
 * @param faixas which depth band each seat falls in, same index as [posicoes]. Zero is the far
 * wall and [FAIXAS_DE_PROFUNDIDADE] - 1 is the near end.
 * @param inicios where each bancada's slice starts, with the total appended, so slice `i` is
 * `inicios[i] until inicios[i + 1]`.
 * @param raioDoAssento the base size of a seat, before [escalaDaFaixa] tapers it by depth.
 */
class HemicicloLayout(
    val posicoes: List<Ponto>,
    val faixas: IntArray,
    val inicios: IntArray,
    val raioDoAssento: Float,
)

/** A plain pair, so the geometry can be computed and tested without a Compose dependency. */
data class Ponto(val x: Float, val y: Float)

/**
 * Spreads a house across rows of an arc.
 *
 * Rows further out are longer, so they hold more seats: the capacity of a row is taken as
 * proportional to its radius, which is what makes the dots come out evenly spaced instead of
 * crowded on the inside and sparse on the outside.
 *
 * The rounding is fixed up at the end rather than ignored. Proportional rounding over sixteen
 * rows of 513 seats lands two or three short, and an arc that draws 510 of 513 is wrong in
 * exactly the way a seat chart must not be — the count is the whole point.
 */
internal fun assentosPorFileira(total: Int, fileiras: Int, raioInterno: Float): List<Int> {
    if (total <= 0 || fileiras <= 0) return emptyList()

    val larguras = (0 until fileiras).map { raioInterno + it * (1f - raioInterno) / fileiras }
    val capacidade = larguras.sum()

    val bruto = larguras.map { (total * it / capacidade) }
    val arredondado = bruto.map { it.toInt() }.toMutableList()

    // Hand out what the flooring dropped, to the rows with the biggest remainder first: the
    // outer rows have the most room for one more and lose the least by not getting it.
    var restante = total - arredondado.sum()
    val ordem = bruto.indices.sortedByDescending { bruto[it] - arredondado[it] }
    var i = 0
    while (restante > 0) {
        arredondado[ordem[i % ordem.size]]++
        restante--
        i++
    }

    return arredondado
}

/**
 * The whole chart: every seat placed and sized, and where each bancada's slice begins.
 *
 * Called once per size change and once per bench change, never per frame and never per
 * highlight — the highlight is a colour and an offset applied to a slice of what this returns.
 *
 * **The chamber is squashed vertically**, so the far rows bunch toward a horizon, and a seat
 * at the near end is drawn a little larger than one at the far wall. Depth is `sin(angulo)` —
 * one at the top of the arc, which is the far wall, and zero at the two ends, which are the
 * benches nearest the observer. "A little" is the whole design: this taper was 55% once, and a
 * size difference that big stops reading as distance and starts reading as light falling
 * unevenly on the chart. See [ESCALA_FUNDO].
 *
 * Seats are sorted by angle first, so walking the list walks the arc from one end to the
 * other. Without that a party's seats would be scattered across rows and the chart would read
 * as noise rather than as blocks.
 */
internal fun hemicicloLayout(
    bancadas: List<Bancada>,
    largura: Float,
    altura: Float,
    fileiras: Int = FILEIRAS,
    raioInterno: Float = RAIO_INTERNO,
): HemicicloLayout {
    val total = bancadas.sumOf { it.assentos }
    if (total <= 0 || largura <= 0f || altura <= 0f) {
        return HemicicloLayout(emptyList(), IntArray(0), intArrayOf(0), 0f)
    }

    val porFileira = assentosPorFileira(total, fileiras, raioInterno)

    // The vertical half-axis is the squashed one, so the arc has to fit `altura` after the
    // squash rather than before it — otherwise the chart leaves a band of empty box on top.
    val raioExterno = minOf(largura / 2f, altura / ACHATAMENTO)
    val centroX = largura / 2f
    val centroY = altura

    val comAngulo = ArrayList<Triple<Float, Ponto, Int>>(total)

    porFileira.forEachIndexed { fileira, assentos ->
        if (assentos <= 0) return@forEachIndexed

        val fracao = raioInterno + fileira * (1f - raioInterno) / fileiras
        val raio = raioExterno * fracao

        for (assento in 0 until assentos) {
            // Half a step in from each end, so the first and last dots of a row are not
            // welded to the baseline.
            val t = (assento + 0.5f) / assentos
            val angulo = (PI - t * PI).toFloat()
            val profundidade = sin(angulo)

            comAngulo += Triple(
                angulo,
                Ponto(
                    x = centroX + raio * cos(angulo),
                    y = centroY - raio * profundidade * ACHATAMENTO,
                ),
                faixaDeProfundidade(profundidade),
            )
        }
    }

    // Descending, because the arc is walked from left (pi) to right (zero).
    comAngulo.sortByDescending { it.first }

    val inicios = IntArray(bancadas.size + 1)
    var acumulado = 0
    bancadas.forEachIndexed { index, bancada ->
        inicios[index] = acumulado
        acumulado += bancada.assentos
    }
    inicios[bancadas.size] = acumulado

    val espacoAngular = PI.toFloat() / (porFileira.maxOrNull() ?: 1)
    val espacoRadial = raioExterno * (1f - raioInterno) / fileiras

    return HemicicloLayout(
        posicoes = comAngulo.map { it.second },
        faixas = IntArray(comAngulo.size) { comAngulo[it].third },
        inicios = inicios,
        raioDoAssento = minOf(
            raioExterno * raioInterno * espacoAngular / 2f,
            espacoRadial / 2f,
        ) * OCUPACAO,
    )
}

/**
 * Which depth band a seat falls in, from its depth: zero at the far wall, the last band at the
 * near end.
 *
 * Perspective wants a size per seat and `drawPoints` takes one size per call, so the taper is
 * quantised into a handful of bands. The count follows the taper: widen the range and the same
 * number of bands starts showing as concentric rings, which is the one artefact this has to
 * avoid — a ring is a line the chart draws that the chamber does not have.
 */
internal fun faixaDeProfundidade(profundidade: Float): Int {
    val perto = (1f - profundidade).coerceIn(0f, 1f)

    return (perto * (FAIXAS_DE_PROFUNDIDADE - 1))
        .roundToInt()
        .coerceIn(0, FAIXAS_DE_PROFUNDIDADE - 1)
}

/** The size a band is drawn at: the middle of the band, so the error is half a step. */
internal fun escalaDaFaixa(faixa: Int): Float =
    ESCALA_FUNDO + (faixa + 0.5f) / FAIXAS_DE_PROFUNDIDADE * (1f - ESCALA_FUNDO)

/**
 * Which bancada a seat belongs to, by binary search over the slice boundaries.
 *
 * Linear would be fine for twenty-two parties; this is here because it is also what a tap
 * uses, and a tap happens while a finger is moving.
 */
internal fun bancadaDoAssento(inicios: IntArray, assento: Int): Int {
    if (assento < 0 || inicios.size < 2 || assento >= inicios.last()) return -1

    var baixo = 0
    var alto = inicios.size - 2

    while (baixo <= alto) {
        val meio = (baixo + alto) / 2
        when {
            assento < inicios[meio] -> alto = meio - 1
            assento >= inicios[meio + 1] -> baixo = meio + 1
            else -> return meio
        }
    }

    return -1
}

/**
 * Sixteen, and the number is a trade rather than a taste.
 *
 * Fewer rows means more seats per row, and the seats of a row are spaced by angle — so the
 * innermost row, which is the shortest, is what decides how big a dot can be. Twelve rows put
 * about fifty-five seats in the longest one and left dots of three points, which is a smear.
 * Sixteen brings it to thirty-six and the dot to something you can see, and past that the rows
 * themselves start to touch.
 */
private const val FILEIRAS = 16

/**
 * Where the innermost row sits, as a fraction of the outer radius.
 *
 * Also a dot-size decision: a bigger hole is a longer inner row and a bigger dot, but past
 * about half the arc stops reading as a chamber and starts reading as a rainbow.
 */
private const val RAIO_INTERNO = 0.42f

/** How much of the space between seats the dot fills, leaving the rest as the gap. */
private const val OCUPACAO = 0.82f

/**
 * How far the arc is squashed vertically.
 *
 * This is the whole of the perspective, geometrically: a circle seen from its own plane is a
 * line and seen from above is a circle, and the chamber is somewhere in between. Lowered from
 * 0.72 — the eye sits nearer the floor now, which makes the view sharper and, because the
 * height of this component is the width times this number over two, also makes it shorter.
 * Much under a half and the far rows start to merge into one another.
 */
internal const val ACHATAMENTO = 0.52f

/**
 * How small a seat at the far wall is drawn, against one at the near end.
 *
 * This number has been walked in from both ends. At 0.45 the taper stopped reading as distance
 * and started reading as light: small dots render washed out against big ones, so the arc came
 * out looking shaded, which is a claim about lighting and not about the chamber. At 0.85 it
 * was honest but barely there. Here the near benches are visibly nearer and the dots at the
 * far wall are still solid dots rather than smudges.
 */
internal const val ESCALA_FUNDO = 0.72f

/**
 * Six, raised with the taper.
 *
 * Bands are a step, and the step is the range divided by this. At the old range four bands put
 * under four per cent between neighbours; at this one they would put seven, which is enough to
 * come out as rings in the arc.
 */
internal const val FAIXAS_DE_PROFUNDIDADE = 6
