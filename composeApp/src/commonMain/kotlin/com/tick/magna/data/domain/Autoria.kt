package com.tick.magna.data.domain

/**
 * Who signed a proposition, in the three shapes that actually occur.
 *
 * Sampled over a 90-day window, 190 of 197 propositions have exactly one author. The seven
 * that do not are led by the single PEC, which needs 171 signatures and had 172 authors — and
 * that rare case is what the card was designed around: a row of up to seven stacked avatars,
 * which in 96% of cards drew one lonely circle.
 *
 * The third shape had no design at all. A third of the PLs in the window, every MSC and most
 * PDLs are signed by an orgao rather than a deputado; the card resolved the author's uri
 * against the local Deputado table, found nothing, and rendered an empty row.
 */
data class Autoria(
    val nome: String,
    val tipo: TipoAutor,
    /** How many signatures there are beyond [nome]. Zero in 96% of cases. */
    val outros: Int,
)

enum class TipoAutor {
    DEPUTADO,

    /** A comissao, the Mesa, the Executive, the Senate: anything with no face to show. */
    ORGAO,
}

/**
 * @return null when the API gave no name, which is the only case the card has nothing to draw.
 *
 * An unrecognised [tipo] is read as an orgao rather than a deputado, because the failure modes
 * are not symmetric: an orgao drawn with a person's icon is wrong on screen, while a deputado
 * drawn with an institution's icon is merely plain.
 */
fun autoriaDe(nome: String?, tipo: String?, total: Int?): Autoria? {
    val limpo = nome?.trim().orEmpty()
    if (limpo.isEmpty()) return null

    return Autoria(
        nome = limpo,
        tipo = if (tipo?.trim()?.startsWith("Deputad", ignoreCase = true) == true) {
            TipoAutor.DEPUTADO
        } else {
            TipoAutor.ORGAO
        },
        // A total smaller than one signature is a total that was not sent, not a negative one.
        outros = ((total ?: 1) - 1).coerceAtLeast(0),
    )
}
