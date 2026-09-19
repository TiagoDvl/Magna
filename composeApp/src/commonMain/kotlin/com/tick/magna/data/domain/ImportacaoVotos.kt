package com.tick.magna.data.domain

/**
 * The state of the optional full-year download, which is the only thing in this app that
 * transfers megabytes.
 *
 * The rules it encodes were decided with the weight in hand: only the current year is offered,
 * the size is stated before anything is transferred, and once downloaded the screen says how
 * complete the data actually is rather than when it was fetched.
 */
sealed interface ImportacaoVotos {

    /**
     * Not offered at all. The download is a file per calendar year, and a past term is five of
     * them and around 150 MB — so a term that has ended shows what it has and says why there
     * is no more, instead of a button nobody should press.
     */
    data object Indisponivel : ImportacaoVotos

    /** Never downloaded. [bytes] is what the screen has to say out loud before asking. */
    data class Disponivel(val bytes: Long) : ImportacaoVotos

    /**
     * Downloaded. [completoAte] is the file's own publication date, not the moment of the
     * download: the files are regenerated nightly, so a vote cast this afternoon is not in the
     * copy on this phone whatever time it was fetched.
     */
    data class Completa(
        val completoAte: String?,
        val votos: Long,
        /** True when a `HEAD` found a newer snapshot. Free to ask, so the button only appears when there is something to get. */
        val desatualizada: Boolean,
        val bytes: Long,
    ) : ImportacaoVotos
}
