package com.tick.magna.util

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Formats an amount as Brazilian currency: `8000.0` becomes `R$ 8.000,00`.
 *
 * Hand written because there is no multiplatform number formatter, and because the app
 * used to store the amount already formatted, which made it impossible to add up.
 */
fun Double.toBrlString(): String {
    if (isNaN() || isInfinite()) return "$CURRENCY_SYMBOL 0,00"

    val totalCents = (this * 100).roundToLong()
    val cents = abs(totalCents)
    val units = cents / 100
    val fraction = cents % 100

    return buildString {
        if (totalCents < 0) append('-')
        append(CURRENCY_SYMBOL)
        append(' ')
        append(units.groupThousands())
        append(',')
        append(fraction.toString().padStart(2, '0'))
    }
}

private fun Long.groupThousands(): String =
    toString()
        .reversed()
        .chunked(THOUSANDS_GROUP)
        .joinToString(THOUSANDS_SEPARATOR)
        .reversed()

private const val CURRENCY_SYMBOL = "R$"
private const val THOUSANDS_SEPARATOR = "."
private const val THOUSANDS_GROUP = 3
