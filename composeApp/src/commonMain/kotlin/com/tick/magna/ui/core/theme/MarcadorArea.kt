package com.tick.magna.ui.core.theme

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The shape that says which part of the Camara something belongs to.
 *
 * Colour alone does not carry it: six accents inside one institutional palette are not six
 * distinguishable hues, and a reader who cannot separate the greens from the teals is left
 * with nothing. A shape is read before a colour is compared, and it survives greyscale.
 *
 * Two are settled. The rest draw nothing until they are, because a marker invented to fill a
 * slot is a marker nobody can learn.
 */
enum class MarcadorArea {
    /** A triangle folded into the top-right corner. Costs no layout: it is drawn, not spaced. */
    CUNHA,

    /** A bar down the left edge, which the proposition card has carried since it was written. */
    TARJA,

    NENHUM,
}

val MagnaArea.marcador: MarcadorArea
    get() = when (this) {
        MagnaArea.DEPUTADOS -> MarcadorArea.CUNHA
        MagnaArea.PROPOSICOES -> MarcadorArea.TARJA
        MagnaArea.PARTIDOS,
        MagnaArea.COMISSOES,
        MagnaArea.VOTACOES,
        MagnaArea.LEGISLATURA -> MarcadorArea.NENHUM
    }

/**
 * Draws [area]'s marker over this element, clipped to [shape].
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
 * It draws on top of the content rather than behind it, because behind a Card's own background
 * is invisible. Nothing is drawn for an area with no marker yet.
 */
@Composable
fun Modifier.marcadorDeArea(
    area: MagnaArea,
    shape: Shape = RectangleShape,
    tamanho: Dp = MARCADOR,
): Modifier {
    val marcador = area.marcador
    if (marcador == MarcadorArea.NENHUM) return this

    val cor = area.accent

    return clip(shape).drawWithContent {
        drawContent()

        when (marcador) {
            MarcadorArea.CUNHA -> {
                val lado = tamanho.toPx()
                val caminho = Path().apply {
                    moveTo(size.width, 0f)
                    lineTo(size.width, lado)
                    lineTo(size.width - lado, 0f)
                    close()
                }
                drawPath(caminho, cor)
            }

            MarcadorArea.TARJA -> drawRect(
                color = cor,
                topLeft = Offset.Zero,
                size = size.copy(width = TARJA.toPx()),
            )

            MarcadorArea.NENHUM -> Unit
        }
    }
}

/** Big enough to read on a 96dp card, small enough not to reach the content on a 200dp one. */
private val MARCADOR = 20.dp

private val TARJA = 4.dp

/** Keeps content clear of a marker that is drawn over it rather than beside it. */
@Composable
fun Modifier.respeitandoMarcador(area: MagnaArea, tamanho: Dp = MARCADOR): Modifier =
    if (area.marcador == MarcadorArea.CUNHA) padding(end = tamanho / 2) else this
