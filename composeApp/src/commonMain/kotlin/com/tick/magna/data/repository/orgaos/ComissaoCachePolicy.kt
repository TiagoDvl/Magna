package com.tick.magna.data.repository.orgaos

import com.tick.magna.data.source.local.dao.ComissaoConteudo

/**
 * Whether what is stored for a committee is still worth showing without asking again.
 *
 * Three rules, in order:
 *
 * A term that has already ended never changes. The 56th legislature finished in January 2023
 * and its committees will not vote again or elect another president, so once downloaded it is
 * correct forever — which also means that browsing old terms, the thing block 8 made possible,
 * costs the network exactly once.
 *
 * A stamp in the future is stale. The device clock is not trustworthy: one jump forward and
 * back would otherwise freeze a cache written "tomorrow" until tomorrow arrives.
 *
 * Otherwise it is an age limit, and the three contents do not share one. The votes are the
 * expensive half of the screen and the only part that moves week to week; a composition is
 * renewed once a legislative year and a presidency changes about as often.
 *
 * Absent is not the same as empty, and that distinction is why there is a stamp at all: the
 * CASP has no votes, so its cache is legitimately three rows of nothing, and without a record
 * that the fetch happened it would re-download that nothing on every visit.
 */
internal fun isComissaoCacheFresh(
    fetchedAt: Long?,
    now: Long,
    conteudo: ComissaoConteudo,
    termHasEnded: Boolean,
): Boolean {
    if (fetchedAt == null) return false
    if (termHasEnded) return true
    if (now < fetchedAt) return false

    return now - fetchedAt < conteudo.maxAge
}

private val ComissaoConteudo.maxAge: Long
    get() = when (this) {
        ComissaoConteudo.VOTACOES -> SIX_HOURS
        ComissaoConteudo.COMPOSICAO, ComissaoConteudo.PRESIDENCIA -> SEVEN_DAYS
    }

private const val SIX_HOURS = 6 * 60 * 60 * 1000L
private const val SEVEN_DAYS = 7 * 24 * 60 * 60 * 1000L
