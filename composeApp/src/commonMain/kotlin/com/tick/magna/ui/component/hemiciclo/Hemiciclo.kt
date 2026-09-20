package com.tick.magna.ui.component.hemiciclo

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.tick.magna.ui.core.theme.MagnaArea
import com.tick.magna.ui.core.theme.accent

/**
 * The whole house as seats, seen from the floor, with any number of benches picked out.
 *
 * **Why it is one Canvas and not 513 composables.** Five hundred and thirteen `Box`es is five
 * hundred and thirteen layout nodes measured and placed on every pass, and a highlight would
 * recompose all of them. This is one draw node: the geometry is computed once per size and the
 * seats are handed to `drawPoints` in a handful of calls.
 *
 * **Why a call per bench per depth band.** The house was two `drawPoints` calls once, and
 * both reasons it is not any more are real. A lifted bench has to leave the floor it came
 * from, and it cannot be subtracted from a list flattened before the selection existed — so
 * the house is drawn bench by bench and a rising one is simply not drawn. And the near seats
 * are a touch larger than the far ones, which `drawPoints` charges for by the size: one call
 * per [FAIXAS_DE_PROFUNDIDADE]. A hundred-odd calls over five hundred and thirteen points,
 * which is the same points either way.
 *
 * **Why the selection arrives as a lambda.** It is read inside the draw block, so the repaint
 * costs no recomposition, and every lift is an `Animatable` read in the same place — sixty
 * frames a second of movement without one recomposition. The selection does reach composition
 * once, as the key that starts the animations; what it never does is reach it per frame, and
 * the buckets survive it either way because they are remembered against the layout.
 *
 * **Why a set and not one bench.** Comparing two benches is the question this chart is for, and
 * a chart that can only answer "how big is the PT" answers it against a house rather than
 * against the PL. Each chosen bench gets its own lift, so a bench added while another is
 * already up rises on its own instead of appearing airborne.
 *
 * **Why one tone for the whole house, and the party's own for a chosen bench.** Twenty-seven
 * hues at once are not twenty-seven distinguishable colours, and in an app about politics a
 * colour handed out to every party is read as a statement about all of them. Untouched, the
 * house is five hundred and thirteen seats and nothing else. But a bench somebody picked is a
 * bench somebody asked about, and there it is worth saying which one — in the colour taken
 * from that party's own logo, the same one its name is written in everywhere else. Where the
 * register hosts no logo that colour is the area's blue, which is what it was before.
 *
 * **What a chosen bench does, and what it deliberately no longer does.** It rises, it grows,
 * and it goes opaque. Gone with it: the fade of the rest of the house, the shadow on the
 * floor, and the seat that shrank with how far back it sat. All three were depth, and stacked
 * on top of one another they came out as shading over the chart rather than distance inside
 * it — a house lit unevenly, which is not a fact about the house. Depth can come back one
 * piece at a time, each earning its place against a chart that already reads.
 */
@Composable
fun Hemiciclo(
    bancadas: List<Bancada>,
    /** One per bancada, same order. Short or empty falls back to the area's own colour. */
    cores: List<Color>,
    selecionadas: () -> Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val acento = MagnaArea.PARTIDOS.accent

    val casa = acento.copy(alpha = ALFA_CASA)

    var tamanho by remember { mutableStateOf(IntPair(0, 0)) }

    val layout = remember(bancadas, tamanho) {
        hemicicloLayout(bancadas, tamanho.largura.toFloat(), tamanho.altura.toFloat())
    }

    // Everything the draw needs, grouped once. A highlight reads from here; it never rebuilds
    // it.
    val grupos = remember(layout) { Grupos(layout) }

    // One per bench, allocated once and never replaced, so the draw block can walk them by
    // index without a map lookup and without ever touching a collection that is being mutated
    // underneath it. `Animatable` rather than `animateFloatAsState` because the value is read
    // in the draw block: a `State` read would land in composition and recompose this on every
    // frame of the animation.
    val elevacoes = remember(bancadas) { List(bancadas.size) { Animatable(0f) } }

    val escolhidas = selecionadas()

    // One effect per bench, and that is the whole point rather than a style choice. A single
    // effect over the set is cancelled and restarted every time the set changes, which kills
    // whatever animations were in flight — and a bench whose target had not changed was then
    // never relaunched, so toggling two benches in quick succession left the first one stuck
    // wherever it happened to be. Keyed per bench, touching one bench cannot cancel another.
    bancadas.forEachIndexed { index, bancada ->
        val alvo = if (bancada.sigla in escolhidas) 1f else 0f

        key(bancada.sigla) {
            LaunchedEffect(elevacoes, alvo) {
                elevacoes[index].animateTo(alvo, tween(DURACAO_MS, easing = FastOutSlowInEasing))
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(ASPECTO)
            .onSizeChanged { tamanho = IntPair(it.width, it.height) }
            .pointerInput(layout, bancadas) {
                detectTapGestures { toque ->
                    val assento = assentoMaisProximo(layout, toque)
                    val index = bancadaDoAssento(layout.inicios, assento)

                    bancadas.getOrNull(index)?.let { onToggle(it.sigla) }
                }
            }
            .drawWithCache {
                val levantamento = LEVANTAMENTO.toPx()

                onDrawBehind {
                    if (layout.posicoes.isEmpty()) return@onDrawBehind

                    // The floor. A bench that is on its way up is faded out of it by exactly
                    // as much as it has risen, so the seats leave rather than duplicate: at
                    // the top of the movement its place in the arc is empty.
                    grupos.porBancada.forEachIndexed { index, porFaixa ->
                        val subida = elevacoes.getOrNull(index)?.value ?: 0f
                        if (subida >= 1f) return@forEachIndexed

                        desenhar(
                            porFaixa = porFaixa,
                            raio = layout.raioDoAssento,
                            cor = if (subida <= 0f) {
                                casa
                            } else {
                                casa.copy(alpha = casa.alpha * (1f - subida))
                            },
                        )
                    }

                    elevacoes.forEachIndexed { index, elevacao ->
                        val subida = elevacao.value
                        if (subida <= 0f || index >= grupos.porBancada.size) {
                            return@forEachIndexed
                        }

                        translate(top = -subida * levantamento) {
                            desenhar(
                                porFaixa = grupos.porBancada[index],
                                raio = layout.raioDoAssento * (1f + subida * CRESCIMENTO),
                                cor = cores.getOrNull(index) ?: acento,
                            )
                        }
                    }
                }
            },
    )
}

/** One call per depth band, which is what the taper costs. */
private fun DrawScope.desenhar(porFaixa: Array<List<Offset>>, raio: Float, cor: Color) {
    porFaixa.forEachIndexed { faixa, pontos ->
        if (pontos.isEmpty()) return@forEachIndexed

        drawPoints(
            points = pontos,
            pointMode = PointMode.Points,
            color = cor,
            strokeWidth = raio * 2f * escalaDaFaixa(faixa),
            cap = StrokeCap.Round,
        )
    }
}

/**
 * The seats of each bench, split by depth band, as the draw wants them.
 *
 * Nothing is flattened across benches, because a bench has to be liftable and fadeable on its
 * own. Nothing is flattened across bands either, because a band is a stroke width. Building
 * this on selection would be cheap too, but it would have to happen in composition, and the
 * whole design of this component is that a selection never reaches composition.
 */
private class Grupos(layout: HemicicloLayout) {
    val porBancada: Array<Array<List<Offset>>> = Array(
        (layout.inicios.size - 1).coerceAtLeast(0),
    ) { bancada ->
        val porFaixa = Array(FAIXAS_DE_PROFUNDIDADE) { mutableListOf<Offset>() }

        for (assento in layout.inicios[bancada] until layout.inicios[bancada + 1]) {
            val ponto = layout.posicoes[assento]

            porFaixa[layout.faixas[assento]] += Offset(ponto.x, ponto.y)
        }

        Array<List<Offset>>(FAIXAS_DE_PROFUNDIDADE) { porFaixa[it] }
    }
}

/**
 * The seat nearest a tap.
 *
 * Five hundred and thirteen squared distances, once per tap. An angular hit test would be
 * cheaper and wrong at the ends of each row, where the seats of two benches interleave.
 */
private fun assentoMaisProximo(layout: HemicicloLayout, toque: Offset): Int {
    var melhor = -1
    var menor = Float.MAX_VALUE

    layout.posicoes.forEachIndexed { index, ponto ->
        val dx = ponto.x - toque.x
        val dy = ponto.y - toque.y
        val distancia = dx * dx + dy * dy

        if (distancia < menor) {
            menor = distancia
            melhor = index
        }
    }

    return melhor
}

/** A stable key for `remember`, which `IntSize` would also be — this one is explicit. */
private data class IntPair(val largura: Int, val altura: Int)

/**
 * The arc's own bounding box, so the component is never taller than what it draws.
 *
 * A fixed height left a band of empty box above the arc on a phone and would have clipped it
 * on anything wider. The width sets the radius — half of it — and the squash sets the height,
 * so the ratio is two over the squash and holds at any width.
 */
private val ASPECTO = 2f / ACHATAMENTO

/**
 * How far a chosen bench leaves the floor.
 *
 * Raised from ten. At a shallower viewing angle a small lift reads as a nudge sideways rather
 * than as height, because the vertical axis now carries less distance per pixel.
 */
private val LEVANTAMENTO = 22.dp

private const val DURACAO_MS = 320

/**
 * The one tone every unchosen seat is drawn in.
 *
 * Between the two the house used to alternate between, and far enough under full that a chosen
 * bench going opaque is unmistakable even before it moves.
 */
private const val ALFA_CASA = 0.45f

/** How much a lifted seat grows, because closer is bigger. */
private const val CRESCIMENTO = 0.25f
