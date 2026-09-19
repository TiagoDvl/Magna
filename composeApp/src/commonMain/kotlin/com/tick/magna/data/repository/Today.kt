package com.tick.magna.data.repository

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.LocalDate

/**
 * Today's date, in UTC.
 *
 * Neither `kotlinx.datetime.Clock` nor `kotlinx.datetime.Instant` survives to runtime here:
 * both compile and then die with NoClassDefFoundError, because the standard library took those
 * two over and the kotlinx-datetime ones are aliases with no class behind them. `LocalDate` is
 * still a real class, which is why the conversion goes through epoch days rather than through
 * an instant and a time zone.
 *
 * UTC rather than the device's zone, which shifts the answer by at most one day. Everything
 * this feeds — which three-month windows of a mandate to count votes in, which slice of a term
 * propositions were filed in — is measured in months, so a day either way changes nothing.
 * Anything that needs the local day must not use this.
 */
@OptIn(ExperimentalTime::class)
internal fun today(): LocalDate {
    val epochDays = Clock.System.now().toEpochMilliseconds() / MILLIS_PER_DAY

    return LocalDate.fromEpochDays(epochDays.toInt())
}

/**
 * The wall clock, in epoch milliseconds, used to decide whether a cache is stale.
 *
 * It is the device's clock and it can be wrong, which is why the rule that reads this treats
 * a stamp in the future as stale rather than as very fresh. See `isComissaoCacheFresh`.
 */
@OptIn(ExperimentalTime::class)
internal fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

private const val MILLIS_PER_DAY = 86_400_000L
