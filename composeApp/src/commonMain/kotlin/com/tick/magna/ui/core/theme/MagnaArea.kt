package com.tick.magna.ui.core.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.HowToVote
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The parts of the Camara this app is made of, each with an identity it keeps everywhere.
 *
 * Until now a section was a green title and nothing else, so "Deputados recentes", "Partidos"
 * and "Comissões Permanentes" were the same object with different words — the app gave no
 * signal about which part of the institution you were looking at.
 *
 * **The icon is the marker, not the colour.** Six icons are unmistakable; six colours are not,
 * and in an app about politics a colour is read as an affiliation whether or not it was meant
 * as one. The existing palette says as much in its own comments: green, gold and blue, chosen
 * from the flag and described as non-partisan. So the colour here is a supporting accent, kept
 * within that institutional range and deliberately never used as a fill behind a party or a
 * deputado.
 */
enum class MagnaArea {
    DEPUTADOS,
    PARTIDOS,
    PROPOSICOES,
    COMISSOES,
    VOTACOES,
    LEGISLATURA,
}

val MagnaArea.icon: ImageVector
    get() = when (this) {
        MagnaArea.DEPUTADOS -> Icons.Outlined.Groups
        MagnaArea.PARTIDOS -> Icons.Outlined.Flag
        MagnaArea.PROPOSICOES -> Icons.Outlined.Description
        MagnaArea.COMISSOES -> Icons.Outlined.AccountBalance
        MagnaArea.VOTACOES -> Icons.Outlined.HowToVote
        MagnaArea.LEGISLATURA -> Icons.Outlined.CalendarMonth
    }

/**
 * The accent of an area.
 *
 * Three of the six reuse the theme's own roles rather than inventing a colour, because they
 * already had one in practice: the app is green, proposals are where the gold shows up, and
 * the blue is what the party charts already use. The two added hues are a muted teal and a
 * bronze, both far from any party palette and both readable on the cream background in light
 * and on the near-black in dark.
 *
 * The legislature has no colour of its own on purpose. It is the frame around everything else,
 * not another thing to look at.
 */
val MagnaArea.accent: Color
    @Composable
    @ReadOnlyComposable
    get() {
        val scheme = MaterialTheme.colorScheme
        val dark = scheme.background.luminanceIsDark()

        return when (this) {
            MagnaArea.DEPUTADOS -> scheme.primary
            MagnaArea.PARTIDOS -> scheme.tertiary
            MagnaArea.PROPOSICOES -> scheme.secondary
            MagnaArea.COMISSOES -> if (dark) comissoesDark else comissoesLight
            MagnaArea.VOTACOES -> if (dark) votacoesDark else votacoesLight
            MagnaArea.LEGISLATURA -> scheme.onSurfaceVariant
        }
    }

/**
 * Whether the scheme in use is the dark one, read off the background rather than passed in.
 *
 * The theme does not expose `isDark`, and threading it through every call site to colour an
 * icon would be worse than this.
 */
private fun Color.luminanceIsDark(): Boolean = (red + green + blue) < DARK_THRESHOLD

private const val DARK_THRESHOLD = 1.5f

/**
 * The area's colour as a surface, for chrome that should belong to the feature it frames.
 *
 * A top bar in this colour and a control block in it below make one continuous field rather
 * than a band of colour that stops halfway down the screen. The legislature has none, because
 * it is the frame around everything and not a feature of its own.
 */
val MagnaArea.container: Color
    @Composable
    @ReadOnlyComposable
    get() {
        val scheme = MaterialTheme.colorScheme
        val dark = scheme.background.luminanceIsDark()

        return when (this) {
            MagnaArea.DEPUTADOS -> scheme.primaryContainer
            MagnaArea.PARTIDOS -> scheme.tertiaryContainer
            MagnaArea.PROPOSICOES -> scheme.secondaryContainer
            MagnaArea.COMISSOES -> if (dark) comissoesContainerDark else comissoesContainerLight
            MagnaArea.VOTACOES -> if (dark) votacoesContainerDark else votacoesContainerLight
            MagnaArea.LEGISLATURA -> scheme.surfaceContainer
        }
    }

/** What reads on [container]. Every pairing clears 6.4:1. */
val MagnaArea.onContainer: Color
    @Composable
    @ReadOnlyComposable
    get() {
        val scheme = MaterialTheme.colorScheme
        val dark = scheme.background.luminanceIsDark()

        return when (this) {
            MagnaArea.DEPUTADOS -> scheme.onPrimaryContainer
            MagnaArea.PARTIDOS -> scheme.onTertiaryContainer
            MagnaArea.PROPOSICOES -> scheme.onSecondaryContainer
            MagnaArea.COMISSOES -> if (dark) onComissoesContainerDark else onComissoesContainerLight
            MagnaArea.VOTACOES -> if (dark) onVotacoesContainerDark else onVotacoesContainerLight
            MagnaArea.LEGISLATURA -> scheme.onSurface
        }
    }
