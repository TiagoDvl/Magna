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
 * REQ: most of what the Camara emits is procedure, not law.
 */
enum class ProposicaoBucket {
    CONSTITUICAO,
    LEI,
    ATO_LEGISLATIVO,
    TRAMITACAO,
}

/**
 * The bucket of a `siglaTipo`, defaulting to [ProposicaoBucket.TRAMITACAO].
 *
 * The three named lists are closed and short; everything else the API can return is procedure
 * of one kind or another, so the default is the honest answer rather than an "unknown" the UI
 * would then have to draw. The suffixed variants the API emits (`SBT-A`, `EMC-A`, `PARF`,
 * `PRLP`) fall through to it for the same reason.
 */
fun proposicaoBucket(siglaTipo: String): ProposicaoBucket {
    return when (siglaTipo.trim().uppercase()) {
        "PEC" -> ProposicaoBucket.CONSTITUICAO

        // MPV is not a bill and is not voted before it takes effect, but it is in force as law
        // from the day it is published, which is the fact a reader needs from a glance. PLV is
        // what it becomes on conversion.
        "PL", "PLP", "MPV", "PLV", "PLN", "PLC" -> ProposicaoBucket.LEI

        // PDL and PDC are the same instrument under two siglas, and PDS is the Senate's. They
        // are not laws: they are how Congress acts on its own authority, which is why a PDL
        // that suspends a decree sits here and not above.
        "PDL", "PDC", "PDS", "PRC", "PRN" -> ProposicaoBucket.ATO_LEGISLATIVO

        else -> ProposicaoBucket.TRAMITACAO
    }
}
