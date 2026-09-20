package com.tick.magna.ui.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The shape that says which part of the Camara something belongs to.
 *
 * Colour alone does not carry it: six accents inside one institutional palette are not six
 * distinguishable hues, and a reader who cannot separate the greens from the teals is left
 * with nothing. A shape is read before a colour is compared, and it survives greyscale.
 *
 * Both are sashes rather than borders: neither touches the edge it runs along, so a strip of
 * the card's own colour stays outside it. A mark that runs into the edge reads as the edge — a
 * frame, a divider, a selection state. One with the surface still showing past it reads as
 * something laid over the card, which is what it is.
 *
 * Two are settled. The rest draw nothing until they are, because a marker invented to fill a
 * slot is a marker nobody can learn.
 */
enum class MarcadorArea {
    /** A diagonal band across the top-right corner, stopping short of the corner itself. */
    CUNHA,

    /** A full-height band beside the left edge, with the card's colour still outside it. */
    TARJA,

    /** A dashed rule above the bottom edge: a tally being kept rather than a thing being held. */
    TRACEJADO,

    /**
     * Five dots in the bottom-right corner, fading away from it.
     *
     * Borrowed from the seat chart, which arrived at dots on its own: five hundred and
     * thirteen of them are the house, and a handful is a party. The one marker in this set
     * that means something beyond "this area" — the rest are shapes chosen to be told apart.
     *
     * A handful in a corner rather than a rule across the base, which read as a dotted border
     * and drew more attention than the party's own name. The fade is what turns five dots into
     * a gesture: the row does not end, it runs out.
     */
    PONTOS,

    NENHUM,
}

val MagnaArea.marcador: MarcadorArea
    get() = when (this) {
        MagnaArea.DEPUTADOS -> MarcadorArea.CUNHA
        MagnaArea.PROPOSICOES -> MarcadorArea.TARJA
        MagnaArea.VOTACOES -> MarcadorArea.TRACEJADO
        MagnaArea.PARTIDOS -> MarcadorArea.PONTOS
        MagnaArea.COMISSOES,
        MagnaArea.LEGISLATURA -> MarcadorArea.NENHUM
    }

/**
 * Draws [area]'s marker under this element's content, clipped to [shape].
 *
 * **Where it belongs, for now:** the Home, where the areas are mixed and a card has to say
 * which one it is. **Not** on the rows inside a feature screen — in a list where every row is
 * the same area the marker says nothing and repeats forty-five times.
 *
 * It was on the feature's top bar too, and came off: a bar draws edge to edge, so its
 * top-right corner is under the status bar and the wedge landed on the battery icon. Carrying
 * the same identity into a feature screen is a separate problem, and the bar already takes the
 * area's colour.
 *
 * **Apply it to the container inside the Card, not to the Card.** A Card paints its own
 * background inside its modifier chain, so anything this draws from the Card's modifier is
 * covered by it. From the content container the band lands on top of that background and
 * under the children, which is how it passes behind a deputado's photograph instead of
 * across it. That container has to fill the card and be padded *after* this, or the band is
 * measured against the padded box and drifts inward.
 *
 * Nothing is drawn for an area with no marker yet.
 */
@Composable
fun Modifier.marcadorDeArea(
    area: MagnaArea,
    shape: Shape = RectangleShape,
    /** The diagonal's own measurements; the tarja's are fixed. */
    recuo: Dp = RECUO,
    espessura: Dp = ESPESSURA,
): Modifier {
    val marcador = area.marcador
    if (marcador == MarcadorArea.NENHUM) return this

    val cor = area.accent

    return clip(shape).drawWithContent {
        when (marcador) {
            // Four points rather than three: the triangle's own tip is cut off, so the
            // rounded corner of the card shows through above the band.
            MarcadorArea.CUNHA -> {
                val folga = recuo.toPx()
                val largura = espessura.toPx()
                val caminho = Path().apply {
                    moveTo(size.width - folga, 0f)
                    lineTo(size.width, folga)
                    lineTo(size.width, folga + largura)
                    lineTo(size.width - folga - largura, 0f)
                    close()
                }
                drawPath(caminho, cor)
            }

            // Moved off the left edge rather than shortened. It keeps the full height —
            // cutting it top and bottom turned it into a fragment floating in the card; what
            // makes it stop reading as the card's frame is the strip of surface outside it.
            MarcadorArea.TARJA -> drawRect(
                color = cor,
                topLeft = Offset(TARJA_RECUO.toPx(), 0f),
                size = size.copy(width = TARJA_ESPESSURA.toPx()),
            )

            // A different edge, not a different treatment of the same one: a dashed band
            // beside the solid one would be two marks to compare rather than two places to
            // look. Inset upward so the card's colour still turns both bottom corners.
            MarcadorArea.TRACEJADO -> {
                val y = size.height - TRACEJADO_RECUO.toPx()

                drawLine(
                    color = cor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = TRACEJADO_ESPESSURA.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(TRACO.toPx(), VAO.toPx()),
                    ),
                )
            }

            MarcadorArea.PONTOS -> {
                val raio = PONTO.toPx() / 2f
                val passo = PONTO.toPx() + VAO_ENTRE_PONTOS.toPx()
                val y = size.height - PONTOS_RECUO.toPx() - raio
                val direita = size.width - PONTOS_RECUO.toPx() - raio

                // Drawn from the corner inwards, so the strongest dot is the one anchored to
                // it and the rest thin out along the way.
                for (i in 0 until PONTOS_NO_CANTO) {
                    val opacidade = 1f - i * (1f - ALFA_MAIS_FRACO) / (PONTOS_NO_CANTO - 1)

                    drawCircle(
                        color = cor.copy(alpha = cor.alpha * opacidade),
                        radius = raio,
                        center = Offset(direita - i * passo, y),
                    )
                }
            }

            MarcadorArea.NENHUM -> Unit
        }

        drawContent()
    }
}

/**
 * How much of the card is left in its own colour before the band starts.
 *
 * Measured against the corner, not the edge: `shapes.medium` rounds at 12dp, and the arc's
 * nearest point to the corner is only about 5dp along the diagonal. Anything under ten and
 * the clip eats the gap entirely, so the band looks like a plain filled corner again.
 */
private val RECUO = 14.dp

/**
 * Along the edge, not across the band. The diagonal cut makes the band itself about seven
 * tenths of this, which is where a 10 becomes the ~7dp the eye actually measures.
 */
private val ESPESSURA = 10.dp

/** The gap matches the band, so the two read as one gesture rather than two measurements. */
private val TARJA_RECUO = 4.dp

private val TARJA_ESPESSURA = 4.dp

/** Above the bottom edge rather than on it, for the same reason the tarja sits off the left. */
private val TRACEJADO_RECUO = 6.dp

private val TRACEJADO_ESPESSURA = 3.dp

/**
 * Long enough to read as a rule and short enough to read as broken. A 2:2 dash at this weight
 * looks like a printing fault; 8 on 5 reads as deliberate at arm's length.
 */
private val TRACO = 8.dp
private val VAO = 5.dp

/** Seat-sized, so the handful reads as a bench and not as a dotted border. */
private val PONTO = 4.dp
private val VAO_ENTRE_PONTOS = 3.dp
private val PONTOS_RECUO = 10.dp

/** Enough to read as a row, few enough to stay in a corner. */
private const val PONTOS_NO_CANTO = 5

/** Where the far end of the row fades to. Not zero: a dot that vanishes ends the row. */
private const val ALFA_MAIS_FRACO = 0.15f
