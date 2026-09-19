package com.tick.magna.features.proposicoes.list

/**
 * Whether scrolling has reached the point where the next page should be asked for.
 *
 * Extracted because it is the part of infinite scroll that goes wrong quietly: a condition
 * that is true once too often asks the Camara for the same page twice, and one that is true
 * too late stops the list dead at the bottom with pages still to come. Both look like nothing
 * on screen until someone counts requests.
 *
 * @param ultimoVisivel index of the last item drawn, or -1 when nothing is.
 * @param total how many items the list holds.
 * @param margem how many items before the end to start loading, so the page arrives before
 * the reader does.
 */
internal fun deveCarregarMais(
    ultimoVisivel: Int,
    total: Int,
    carregando: Boolean,
    temMais: Boolean,
    margem: Int = MARGEM_PREFETCH,
): Boolean {
    if (carregando || !temMais) return false

    // An empty list is the first page's job, not the second's. Without this the screen would
    // ask for page two while page one was still empty.
    if (total == 0) return false

    return ultimoVisivel >= total - 1 - margem
}

/** Five items of warning, which is a quarter of a page. */
private const val MARGEM_PREFETCH = 5
