package com.tick.magna.ui.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

/**
 * How one screen becomes another.
 *
 * There was nothing here before: every screen in the app appeared and vanished on the same
 * frame, which leaves the person to work out from the content alone whether they went deeper
 * or came back. Movement is the cheapest way to say which — and the only one that survives
 * somebody not reading the screen yet.
 *
 * **The two screens do not travel the same distance.** The one arriving comes the whole way in
 * from the edge; the one leaving slides a fraction of the width and no further, so it reads as
 * staying put underneath rather than being thrown off the other side. That is what makes a
 * push feel like a stack instead of a carousel, and it is why the pop is not simply the enter
 * played backwards.
 *
 * **The fade is shorter than the slide on the way out.** A screen that is still legible while
 * it slides competes with the one arriving; fading it first leaves the movement to the new
 * screen, which is the one the eye should be following.
 */
object Transicoes {

    val entrar: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(
            animationSpec = tween(DURACAO_MS, easing = SAIDA_SUAVE),
            initialOffsetX = { largura -> largura },
        ) + fadeIn(animationSpec = tween(DURACAO_MS))
    }

    val sair: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(
            animationSpec = tween(DURACAO_MS, easing = SAIDA_SUAVE),
            targetOffsetX = { largura -> -(largura * DESLOCAMENTO_DE_BAIXO).toInt() },
        ) + fadeOut(animationSpec = tween(DURACAO_DO_ESMAECIMENTO_MS))
    }

    val voltarEntrando: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(
            animationSpec = tween(DURACAO_MS, easing = SAIDA_SUAVE),
            initialOffsetX = { largura -> -(largura * DESLOCAMENTO_DE_BAIXO).toInt() },
        ) + fadeIn(animationSpec = tween(DURACAO_MS))
    }

    val voltarSaindo: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(
            animationSpec = tween(DURACAO_MS, easing = SAIDA_SUAVE),
            targetOffsetX = { largura -> largura },
        ) + fadeOut(animationSpec = tween(DURACAO_DO_ESMAECIMENTO_MS))
    }
}

/**
 * Long enough to be read as movement, short enough that somebody tapping through four screens
 * never waits for it. Under about 200 it reads as a flicker; over about 400 it reads as the
 * app being slow.
 */
private const val DURACAO_MS = 300

private const val DURACAO_DO_ESMAECIMENTO_MS = 180

/** How far the screen underneath travels, as a fraction of the width. */
private const val DESLOCAMENTO_DE_BAIXO = 0.25f

/** Fast at the start and settling at the end, which is how a thing with weight arrives. */
private val SAIDA_SUAVE = CubicBezierEasing(0.2f, 0f, 0f, 1f)
