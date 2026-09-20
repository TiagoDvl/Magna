package com.tick.magna.features.deputados.details

/**
 * The three ways a vote reads, which is fewer than the five the Camara spells.
 *
 * `Sim` and `Não` are the vote; `Abstenção`, `Obstrução` and `Artigo 17` are all ways of not
 * casting one, and colouring each of them separately would be five colours where the reader
 * only ever asks one question.
 *
 * Matched case-insensitively and with the accent stripped, because the register is not
 * consistent about either and a tag that silently falls through to [OUTRO] would be a wrong
 * answer rather than a missing one.
 */
enum class TomDoVoto { SIM, NAO, OUTRO }

fun tomDoVoto(voto: String): TomDoVoto {
    val limpo = voto.trim().lowercase()

    return when (limpo) {
        "sim" -> TomDoVoto.SIM
        "não", "nao" -> TomDoVoto.NAO
        else -> TomDoVoto.OUTRO
    }
}
