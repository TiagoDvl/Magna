package com.tick.magna.data.repository.votos

/**
 * Whether a votacao is likely to have individual votes attached, read off the text the listing
 * already returns.
 *
 * This exists because asking is expensive and the answer is almost always no. The Camara
 * registered 7360 votacoes in 2026 and **152 of them are nominal** — 2%. Calling
 * `/votacoes/{id}/votos` on all of them to find out would be 7360 requests for 152 answers.
 *
 * There is a perfect discriminator and the API does not expose it: the annual `votacoes` file
 * has `votosSim`/`votosNao`/`votosOutros`, filled on 151 of the 152 nominal votacoes and on
 * none of the 7208 symbolic ones. No endpoint returns those fields, in the listing or in the
 * detail. What does leak is the same number written into the description, which is what this
 * reads.
 *
 * Measured against the 152 nominal votacoes of 2026: this catches 147, with 2 false positives.
 * A false positive costs one request that comes back empty; a miss costs a vote that is not
 * shown until the full-year file is downloaded. Both are better than 7360 requests.
 *
 * `Resultado:` is in here and matters more than it looks: committee votacoes word themselves
 * differently (`Resultado:  17 votos "Sim"`) and dropping it takes the filter from 147 to 122.
 */
internal fun isVotacaoNominal(descricao: String?): Boolean {
    if (descricao == null) return false

    return NOMINAL_MARKERS.any { marker -> descricao.contains(marker) }
}

private val NOMINAL_MARKERS = listOf("Sim:", "Resultado:")
