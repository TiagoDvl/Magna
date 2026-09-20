package com.tick.magna.features.santinho

import com.tick.magna.data.santinho.Santinho

data class SantinhoState(
    /** False where the platform has no keystore. The screen says so instead of pretending. */
    val disponivel: Boolean = true,
    val carregando: Boolean = true,
    /** What is on screen, which is what the person has typed since the last save. */
    val santinho: Santinho = Santinho(),
    /** What is actually on disk, so the screen can tell whether there is anything to save. */
    val guardado: Santinho = Santinho(),
    /**
     * Whether the digits are legible.
     *
     * Hidden by default, and this is the one thing here that was not asked for. Saving is what
     * requires a fingerprint, so the note is guarded against somebody editing it — but the
     * thing worth guarding is reading it, and a screen that opens with five numbers in plain
     * sight is a screen somebody can read over a shoulder in a queue. The toggle costs nothing
     * and puts the choice with the person holding the phone.
     */
    val revelado: Boolean = false,
    /**
     * Whether the banner still belongs on the Home.
     *
     * Closed once and closed for good, which is the only way a dismissal means anything. The
     * way back in is a quiet button beside the app's name — the banner is an offer, and an
     * offer somebody declined should not be made again, but the door it opened stays open.
     */
    val bannerVisivel: Boolean = false,
    /**
     * Whether the shortcut beside the wordmark still owes its one pulse.
     *
     * Held here rather than in the composable, and that is the whole behaviour: this ViewModel
     * is scoped to the Home destination, so it outlives every trip into a detail screen and
     * back. A flag in the composable would fire on every return, which is a thing that is
     * charming twice and irritating after that.
     */
    val brilhoPendente: Boolean = true,
    val aviso: AvisoDoSantinho? = null,
) {
    val alterado: Boolean get() = santinho != guardado
}

/**
 * What to say after something did not happen.
 *
 * [RECUSADO] and [NAO_MOSTRADO] are the same refusal at two different doors, and they are not
 * the same sentence: "nothing was saved" in front of somebody who was only trying to look at
 * their own note would report a change they never asked for.
 */
enum class AvisoDoSantinho { GUARDADO, RECUSADO, NAO_MOSTRADO, FALHOU }
