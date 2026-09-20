package com.tick.magna.ui.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.tick.magna.data.domain.Partido

/**
 * The colour a party is drawn in.
 *
 * One place, because the answer is about to get more interesting than it is today and every
 * screen that names a party should change with it rather than after it.
 *
 * Today it is always the area's own blue. The obvious source for a real one — the logo the
 * register points at — does not exist for ten of the twenty-two parties, and the ten include
 * PL, UNIÃO, REPUBLICANOS and MDB, which between them hold 230 of the 513 seats. Measured
 * against `.gif`, `.png`, `.jpg`, upper case, lower case and the accent stripped: all 404. A
 * scheme that answers for the small parties and not the large ones is worse than one that
 * answers the same for everybody.
 *
 * The green this replaced was not a fallback, it was `colorScheme.primary` — the colour that
 * belongs to deputados, on a card about a party.
 */
val Partido.cor: Color
    @Composable
    @ReadOnlyComposable
    get() = MagnaArea.PARTIDOS.accent
