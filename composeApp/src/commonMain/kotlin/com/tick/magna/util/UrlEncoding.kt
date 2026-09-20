package com.tick.magna.util

/**
 * Percent-encodes everything that is not unreserved, which is all a query value needs.
 *
 * Hand-rolled because commonMain has no URL encoder and the alternative is an expect/actual for
 * four lines. Encodes the UTF-8 bytes, so it stays correct for anything with an accent in it.
 */
internal fun String.percentEncoded(): String = buildString {
    for (byte in this@percentEncoded.encodeToByteArray()) {
        val char = byte.toInt().toChar()

        if (char.isLetterOrDigit() && char.code < ASCII || char in SEGUROS) {
            append(char)
        } else {
            append('%')
            append(byte.toInt().and(BYTE).toString(HEX).uppercase().padStart(2, '0'))
        }
    }
}

private const val ASCII = 128
private const val BYTE = 0xFF
private const val HEX = 16
private const val SEGUROS = "-_.~"
