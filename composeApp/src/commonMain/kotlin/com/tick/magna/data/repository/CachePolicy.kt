package com.tick.magna.data.repository

/**
 * Whether something downloaded earlier is still worth showing without asking again.
 *
 * Three rules, and the first two are what make this shared rather than copied:
 *
 * A term that has already ended never changes. The 56th legislature finished in January 2023
 * and its committees will not vote again or elect another president, so once downloaded it is
 * correct forever — which is what makes browsing old terms, the thing block 8 made possible,
 * cost the network exactly once.
 *
 * A stamp in the future is stale. The device clock is not trustworthy: one jump forward and
 * back would otherwise freeze a cache written "tomorrow" until tomorrow arrives.
 *
 * Absent is not empty. A null stamp means nothing was ever downloaded, which is a different
 * answer from having downloaded nothing — the CASP has no votes at all, and a normal quarter
 * of the plenary has eleven nominal votacoes.
 *
 * [maxAge] is the caller's, because the things this guards do not age alike: plenary votes
 * move week to week, a committee composition is renewed once a legislative year.
 */
internal fun isCacheFresh(
    fetchedAt: Long?,
    now: Long,
    maxAge: Long,
    termHasEnded: Boolean,
): Boolean {
    if (fetchedAt == null) return false
    if (termHasEnded) return true
    if (now < fetchedAt) return false

    return now - fetchedAt < maxAge
}
