package com.tick.magna.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.tick.magna.data.domain.TomDoVoto
import com.tick.magna.data.domain.tomDoVoto
import com.tick.magna.ui.core.theme.LocalDimensions

/**
 * How somebody voted, drawn the same way wherever the vote appears.
 *
 * Two screens show votes — a deputado's record and a votacao's roll — and they had drifted:
 * one drew every vote in the same green pill, the other in the same grey one, so the same
 * `Não` looked like approval on one screen and like nothing on the other.
 *
 * Sim and Não are told apart by colour as well as by the word, in the pairing the plenary's
 * own board uses. It says nothing about whether the votacao passed — that is a separate fact
 * and is deliberately not drawn against the person. Everything that is not a vote —
 * `Abstenção`, `Obstrução`, `Artigo 17` — is neutral, because five colours for one question is
 * not a scale.
 *
 * The word stays the primary signal, so the tag still reads with no colour vision at all.
 */
@Composable
fun VotoTag(voto: String, modifier: Modifier = Modifier) {
    val dimensions = LocalDimensions.current
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val fundo: Color
    val frente: Color

    when (tomDoVoto(voto)) {
        TomDoVoto.SIM -> {
            fundo = colorScheme.primaryContainer
            frente = colorScheme.onPrimaryContainer
        }

        TomDoVoto.NAO -> {
            fundo = colorScheme.errorContainer
            frente = colorScheme.onErrorContainer
        }

        TomDoVoto.OUTRO -> {
            fundo = colorScheme.surfaceContainerHighest
            frente = colorScheme.onSurfaceVariant
        }
    }

    Box(
        modifier = modifier
            .background(color = fundo, shape = MaterialTheme.shapes.extraSmall)
            .padding(horizontal = dimensions.grid8, vertical = dimensions.grid2),
    ) {
        Text(
            text = voto,
            style = typography.labelSmall.copy(color = frente, fontWeight = FontWeight.Bold),
        )
    }
}
