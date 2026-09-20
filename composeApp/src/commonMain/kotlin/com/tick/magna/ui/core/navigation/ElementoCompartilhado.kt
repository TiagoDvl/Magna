package com.tick.magna.ui.core.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.ContentScale

/**
 * The two scopes a shared element needs, handed down instead of threaded through.
 *
 * A shared element has to know both the layout the whole transition happens inside and the
 * particular screen it is currently on. Both come from the navigation graph, and every
 * composable between it and an avatar eight levels down would otherwise have to carry two
 * parameters it has no use for. Locals here, and nothing below the graph mentions either scope
 * by name.
 *
 * Null is the ordinary state, not an error: the previews draw these screens with no navigation
 * around them at all, and a modifier that crashes outside a NavHost is a modifier nobody can
 * put in a component.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Carries this element across a navigation, from wherever it is to wherever [chave] appears
 * next.
 *
 * **The key is the thing, not the place.** `deputado-foto-204521` is the same photograph
 * whether it is 32dp in a carousel, 40dp in a search result, or 128dp at the top of a
 * detail screen, and that is exactly what makes the flight readable: the person tapped a face
 * and the face is what travelled. Keying it by screen instead would need a pair per route and
 * would break the moment a fourth screen showed the same person.
 *
 * Returns the modifier untouched when either scope is missing, which is every preview and
 * anything drawn outside the graph.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.elementoCompartilhado(chave: String): Modifier = composed {
    val sharedScope = LocalSharedTransitionScope.current
    val visibilityScope = LocalAnimatedVisibilityScope.current

    if (sharedScope == null || visibilityScope == null) {
        this
    } else {
        with(sharedScope) {
            this@composed.sharedElement(
                sharedContentState = rememberSharedContentState(key = chave),
                animatedVisibilityScope = visibilityScope,
                boundsTransform = { _, _ -> tween(DURACAO_MS) },
            )
        }
    }
}

/**
 * Text that carries across a navigation, the same way [elementoCompartilhado] carries a shape.
 *
 * **Bounds and not the element itself, which is the whole difference.** `sharedElement` moves
 * one composable and scales it, so a sigla going from 16sp in a card to 36sp on a screen
 * arrives as a magnified picture of 16sp type — the strokes thicken and the letterforms smear.
 * `sharedBounds` animates the box while each end draws its own text at its own size, and
 * `ScaleToBounds` keeps the two readings lined up while the box travels.
 *
 * Only worth it where both ends are the same string. A label that changes wording mid-flight
 * is two labels and should cross-fade like everything else.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.textoCompartilhado(chave: String): Modifier = composed {
    val sharedScope = LocalSharedTransitionScope.current
    val visibilityScope = LocalAnimatedVisibilityScope.current

    if (sharedScope == null || visibilityScope == null) {
        this
    } else {
        with(sharedScope) {
            this@composed.sharedBounds(
                sharedContentState = rememberSharedContentState(key = chave),
                animatedVisibilityScope = visibilityScope,
                resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds(
                    contentScale = ContentScale.FillWidth,
                    alignment = Alignment.CenterStart,
                ),
                boundsTransform = { _, _ -> tween(DURACAO_MS) },
            )
        }
    }
}

/** Keys, in one place, so the two ends of a flight cannot disagree about spelling. */
object ChaveCompartilhada {

    /** A deputado's photograph, from any list that shows it to their own screen. */
    fun fotoDoDeputado(id: String): String = "deputado-foto-$id"

    /**
     * A party's sigla, from either list that shows it to the top of its own screen.
     *
     * The strongest pair in the app and the reason the text variant exists: it is the same
     * three letters, in the same weight, in the party's own colour on both ends. Only the size
     * changes, so nothing has to cross-fade — the word simply grows into place.
     */
    fun siglaDoPartido(id: String): String = "partido-sigla-$id"

    /** A committee's short name, from either list to the title of its own screen. */
    fun nomeDaComissao(id: String): String = "comissao-nome-$id"
}

/**
 * The same length as the screen transition it happens inside. A shared element that finishes
 * early lands and then waits for the screen around it; one that finishes late is still moving
 * after the screen has settled, which reads as a stutter rather than as a flight.
 */
private const val DURACAO_MS = 300
