package com.tick.magna.features.santinho

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HowToVote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import com.tick.magna.ui.core.theme.LocalDimensions
import kotlinx.coroutines.delay
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.santinho_atalho
import org.jetbrains.compose.resources.stringResource

/**
 * The quiet way back to a banner somebody closed, beside the app's name.
 *
 * **It is the app's own colour, not an area's.** Every other coloured thing on the Home belongs
 * to a part of the Camara — the deputados' green, the propositions' gold, the committees' teal,
 * the parties' blue. This belongs to the person, so it takes `primary`: the colour Magna uses
 * for itself, which is also what the first-run spinner uses for the same reason.
 *
 * **It catches the light once, and once is the whole design.** The banner was dismissed, so
 * this is the only thing left on the screen saying the santinho exists, and a small icon in a
 * corner is exactly what somebody stops seeing after a week. But the Home leaves composition
 * every time another screen goes on top of it, so an effect keyed to arriving here would pulse
 * on every single return — charming twice, then a tic.
 *
 * [brilhar] therefore comes from the ViewModel, which is scoped to the Home destination and
 * outlives all of that. It is spent the first time the pulse plays and stays spent.
 */
@Composable
fun AtalhoDoSantinho(
    modifier: Modifier = Modifier,
    brilhar: Boolean,
    onBrilhoMostrado: () -> Unit,
    onClick: () -> Unit,
) {
    val dimensions = LocalDimensions.current
    val cor = MaterialTheme.colorScheme.primary

    val brilho = remember { Animatable(0f) }

    LaunchedEffect(brilhar) {
        if (!brilhar) return@LaunchedEffect

        // After the screen has arrived rather than during it: a pulse competing with the
        // navigation transition is a pulse nobody sees.
        delay(ESPERA_MS)

        brilho.animateTo(1f, tween(SUBIDA_MS, easing = FastOutSlowInEasing))
        brilho.animateTo(0f, tween(DESCIDA_MS, easing = FastOutSlowInEasing))

        onBrilhoMostrado()
    }

    IconButton(
        modifier = modifier.size(dimensions.grid32),
        onClick = onClick,
    ) {
        Icon(
            modifier = Modifier
                .size(dimensions.grid20)
                // Both read the Animatable inside a deferred block, so the pulse costs draw
                // and layer passes and not one recomposition.
                .drawBehind {
                    val intensidade = brilho.value
                    if (intensidade <= 0f) return@drawBehind

                    drawCircle(
                        color = cor.copy(alpha = intensidade * ALFA_DO_HALO),
                        radius = size.minDimension / 2f * (1f + intensidade * CRESCIMENTO_DO_HALO),
                    )
                }
                .graphicsLayer {
                    val escala = 1f + brilho.value * CRESCIMENTO
                    scaleX = escala
                    scaleY = escala
                },
            imageVector = Icons.Outlined.HowToVote,
            tint = cor,
            contentDescription = stringResource(Res.string.santinho_atalho),
        )
    }
}

/** Long enough for the screen to settle, short enough to still read as part of arriving. */
private const val ESPERA_MS = 450L

private const val SUBIDA_MS = 260
private const val DESCIDA_MS = 420

/** A nudge, not a jump. Past about a fifth it reads as a button being pressed by itself. */
private const val CRESCIMENTO = 0.18f

private const val ALFA_DO_HALO = 0.22f

private const val CRESCIMENTO_DO_HALO = 0.9f
