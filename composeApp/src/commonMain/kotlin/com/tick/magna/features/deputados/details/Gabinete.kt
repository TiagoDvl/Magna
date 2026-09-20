package com.tick.magna.features.deputados.details

import com.tick.magna.util.percentEncoded

/**
 * The gabinete's building and room are one address, not two facts.
 *
 * They arrived as separate rows — `Prédio 4` over `Sala 420` — which is the register's shape
 * rather than the reader's: nobody holds a building without a room or a room without a
 * building, and neither half is worth a line of its own.
 *
 * Returns null when there is no room, because a building alone points at four hundred doors.
 */
internal fun salaDoGabinete(predio: String?, sala: String?): SalaDoGabinete? {
    val numeroDaSala = sala?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val anexo = predio?.trim()?.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }

    return SalaDoGabinete(anexo = anexo, sala = numeroDaSala)
}

/**
 * @param anexo null when the register's `predio` is not a number. Measured over twenty
 * deputados it is `4` eighteen times, `3` once and **`x`** once, and "Anexo x" is not a place
 * — neither to print nor to hand to a map.
 */
internal data class SalaDoGabinete(val anexo: String?, val sala: String)

/**
 * Where to send a map, for the annexes that are actually places.
 *
 * The room is left out on purpose: a map cannot find a door inside the building, and adding it
 * to the query is how a search stops matching anything. Written without accents because it
 * goes into a URL and Google resolves `Camara` perfectly well.
 */
internal fun mapaDoGabinete(anexo: String?): String? {
    if (anexo == null) return null

    val consulta = "Camara dos Deputados Anexo $anexo, Praca dos Tres Poderes, Brasilia - DF"

    return "https://www.google.com/maps/search/?api=1&query=" + consulta.percentEncoded()
}

/**
 * The gabinete's telephone as something a phone can dial.
 *
 * The register sends eight digits and no area code — `3215-5420` in all twenty measured — and
 * eight digits are not dialable from anywhere. Every gabinete is in Brasilia, so the missing
 * part is known rather than guessed: +55 61.
 *
 * Anything that is already longer is passed through with only its punctuation removed, and
 * anything that is neither is refused. A `tel:` that cannot complete a call is worse than a
 * line of text.
 */
internal fun telefoneDiscavel(telefone: String?): String? {
    val digitos = telefone?.filter(Char::isDigit).orEmpty()

    return when (digitos.length) {
        LOCAL -> "tel:+55$DDD_BRASILIA$digitos"
        COM_DDD, COM_DDD_NOVE -> "tel:+55$digitos"
        COM_PAIS, COM_PAIS_NOVE -> "tel:+$digitos"
        else -> null
    }
}

/** The same number, written so the area code being added is visible rather than implied. */
internal fun telefoneLegivel(telefone: String?): String? {
    val digitos = telefone?.filter(Char::isDigit).orEmpty()
    if (digitos.length != LOCAL) return telefone?.trim()?.takeIf { it.isNotEmpty() }

    return "($DDD_BRASILIA) ${digitos.take(4)}-${digitos.drop(4)}"
}

internal fun emailDiscavel(email: String?): String? {
    val limpo = email?.trim()?.takeIf { it.isNotEmpty() } ?: return null

    // Not validation, just a floor: an address with no @ is not one, and `mailto:` on it opens
    // a compose window addressed to nothing.
    return if (limpo.contains('@')) "mailto:$limpo" else null
}

private const val DDD_BRASILIA = "61"

private const val LOCAL = 8
private const val COM_DDD = 10
private const val COM_DDD_NOVE = 11
private const val COM_PAIS = 12
private const val COM_PAIS_NOVE = 13
