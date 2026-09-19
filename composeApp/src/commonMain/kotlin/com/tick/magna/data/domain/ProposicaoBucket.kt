package com.tick.magna.data.domain

/**
 * What kind of instrument a proposition is, in four buckets.
 *
 * The Camara's reference table `/referencias/proposicoes/siglaTipo` holds 544 values, and a
 * 2500-item sample of a single 90-day window still turned up more than twenty of them: PRL
 * 448, PL 282, REQ 271, DOC 219, RIC 217, PAR 215. Nothing categorical survives that many
 * values — a palette runs out around six distinguishable hues, and a reader runs out sooner.
 *
 * What does survive is the hierarchy the instruments already sit in. Four buckets separate
 * changing the Constitution from making a law from an act of the houses from the paperwork
 * that moves a proposition along, and that is the distinction someone scanning a list is
 * actually making. It also answers, without anyone asking, why the list is mostly PRL and
 * REQ: most of what the Camara emits is procedure, not law. Measured over one window:
 * 11333 in total, of which 1 Constituicao, 2161 Lei, 323 Ato and 8848 Tramitacao.
 *
 * [siglas] is the whole definition, so the filter and the badge cannot drift apart: the same
 * list that picks an icon is the list sent to the API and to SQL.
 */
enum class ProposicaoBucket(val siglas: List<String>) {
    CONSTITUICAO(listOf("PEC")),

    // MPV is not a bill and is not voted before it takes effect, but it is in force as law
    // from the day it is published, which is the fact a reader needs from a glance. PLV is
    // what it becomes on conversion.
    LEI(listOf("PL", "PLP", "MPV", "PLV", "PLN", "PLC")),

    // PDL and PDC are the same instrument under two siglas, and PDS is the Senate's. They are
    // not laws: they are how Congress acts on its own authority, which is why a PDL that
    // suspends a decree sits here and not above.
    ATO_LEGISLATIVO(listOf("PDL", "PDC", "PDS", "PRC", "PRN")),

    /**
     * Everything else, by subtraction rather than by list.
     *
     * It cannot be enumerated — 544 siglas exist and the app will meet ones written after this
     * file — so it is defined as the complement of the other three. That is also why it has no
     * count of its own from the API and no `IN` clause: it is the `NOT IN`.
     */
    TRAMITACAO(emptyList()),
    ;

    companion object {
        /** The buckets the API can be asked about directly, which is all of them but one. */
        val fechados: List<ProposicaoBucket> get() = entries.filter { it.siglas.isNotEmpty() }

        /** Every sigla any bucket claims: the list [TRAMITACAO] is the complement of. */
        val siglasClassificadas: List<String> get() = fechados.flatMap { it.siglas }
    }
}

/**
 * The bucket of a `siglaTipo`, defaulting to [ProposicaoBucket.TRAMITACAO].
 *
 * The default is the honest answer rather than an "unknown" the UI would then have to draw:
 * everything outside three short closed lists is procedure of one kind or another. The
 * suffixed variants the API emits (`SBT-A`, `EMC-A`, `PARF`, `PRLP`) fall through for the
 * same reason.
 */
fun proposicaoBucket(siglaTipo: String): ProposicaoBucket {
    val sigla = siglaTipo.trim().uppercase()

    return ProposicaoBucket.fechados.firstOrNull { sigla in it.siglas }
        ?: ProposicaoBucket.TRAMITACAO
}
