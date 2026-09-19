package com.tick.magna.data.repository.proposicoes

import com.tick.magna.data.domain.ProposicaoBucket

/**
 * The number beside each chip, including the one the API cannot be asked for.
 *
 * Three of the buckets are a closed list of siglas and the endpoint counts them directly.
 * [ProposicaoBucket.TRAMITACAO] is the complement of those three, and there is no way to ask
 * "everything except these twelve" — so it is what is left of the window once they are taken
 * out. Measured over one window: 11333 total, 1 + 2161 + 323 named, 8848 left over.
 *
 * @param total the whole window, or null if that request failed.
 * @param fechados a count per closed bucket; a missing or null entry is a request that failed.
 * @return counts keyed by bucket, with null keyed to the total. Tramitacao appears only when
 * every other number is known, because a subtraction with a hole in it is not a smaller
 * number — it is a wrong one, and a chip reading `Tramitação 2485` would be believed.
 */
internal fun contagensPorBucket(
    total: Int?,
    fechados: Map<ProposicaoBucket, Int?>,
): Map<ProposicaoBucket?, Int> {
    val contagens = mutableMapOf<ProposicaoBucket?, Int>()

    total?.let { contagens[null] = it }
    fechados.forEach { (bucket, quantidade) -> quantidade?.let { contagens[bucket] = it } }

    val completo = total != null &&
        ProposicaoBucket.fechados.all { fechados[it] != null }

    if (completo) {
        val nomeados = ProposicaoBucket.fechados.sumOf { fechados.getValue(it)!! }

        // Never negative, even though it cannot be: the two numbers come from two requests and
        // the window can be refiled between them.
        contagens[ProposicaoBucket.TRAMITACAO] = (total!! - nomeados).coerceAtLeast(0)
    }

    return contagens
}
