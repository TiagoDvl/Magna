package com.tick.magna.data.santinho

/**
 * The note, as the fixed-width text that gets encrypted.
 *
 * **Every note is the same length, and that is the point.** AES-GCM ciphertext is exactly as
 * long as its plaintext, so a note serialised as "1234,,,," would produce a shorter blob than
 * one with all five races filled in — and anybody reading the database file could tell how
 * much somebody had written down without decrypting a thing. Padding each field to the number
 * of boxes the machine shows makes every note sixteen characters, full or empty.
 *
 * No delimiters for the same reason there are no columns: the widths are fixed and known, so
 * nothing in the plaintext has to mark where one office ends and the next begins.
 */
internal fun Santinho.paraTexto(): String =
    CargoDaUrna.entries.joinToString("") { cargo -> valorDe(cargo).padEnd(cargo.digitos, PREENCHIMENTO) }

/**
 * Back again, tolerating a text that is shorter than it should be.
 *
 * A note written by an older version with fewer offices on it decodes to whatever it does have
 * and blanks for the rest, rather than throwing away everything the person typed.
 */
internal fun santinhoDeTexto(texto: String): Santinho {
    var inicio = 0
    var santinho = Santinho()

    CargoDaUrna.entries.forEach { cargo ->
        val fim = minOf(inicio + cargo.digitos, texto.length)
        val pedaco = if (inicio < fim) texto.substring(inicio, fim) else ""

        santinho = santinho.com(cargo, pedaco.trim())
        inicio += cargo.digitos
    }

    return santinho
}

/** A space, because a zero is a digit somebody might actually have written. */
private const val PREENCHIMENTO = ' '
