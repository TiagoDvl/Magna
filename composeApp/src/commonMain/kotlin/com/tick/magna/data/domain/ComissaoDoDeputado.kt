package com.tick.magna.data.domain

/**
 * One seat, seen from the person rather than from the committee.
 *
 * [MembroComissao] is the same fact written for the committee screen, and carries the dates
 * that decide whether a seat is still held. Here the filtering already happened — what is
 * stored is the composition as of the end of the term — so all that is left is which
 * committee and in what capacity.
 *
 * @param sigla null when the committee's own row has not been downloaded yet. The seat is
 * real either way; it just cannot be named.
 */
data class ComissaoDoDeputado(
    val orgaoId: String,
    val sigla: String?,
    /** As the Camara spells it: `Presidente`, `1º Vice-Presidente`, `Titular`, `Suplente`. */
    val titulo: String,
    /** 1 to 4 for the mesa, 101 titular, 102 suplente. */
    val codTitulo: Int,
) {

    val cargo: CargoComissao
        get() = when (codTitulo) {
            in PRESIDENTE..TERCEIRO_VICE -> CargoComissao.MESA
            SUPLENTE -> CargoComissao.SUPLENTE
            else -> CargoComissao.TITULAR
        }

    val isPresidente: Boolean
        get() = codTitulo == PRESIDENTE

    private companion object {
        const val PRESIDENTE = 1
        const val TERCEIRO_VICE = 4
        const val SUPLENTE = 102
    }
}

/**
 * The seat worth printing when there is room for one.
 *
 * The office decides, not the committee: of the 513 deputados in the 57th, 480 hold a seat
 * somewhere and most hold several — 717 suplente seats against 580 titular ones — so almost
 * every row would otherwise show whichever committee sorted first. `codTitulo` orders the
 * offices already, from president down to suplente, and the sigla only breaks a tie so the
 * answer does not move between reads.
 *
 * A seat whose committee has no sigla loses the tie-break but is still eligible: 33 people
 * hold no seat at all and print nothing, which is a different thing from holding one that
 * cannot be named.
 */
fun List<ComissaoDoDeputado>.principal(): ComissaoDoDeputado? =
    minWithOrNull(compareBy({ it.codTitulo }, { it.sigla ?: "" }))
