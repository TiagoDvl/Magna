package com.tick.magna.features.home

import androidx.compose.runtime.Composable
import com.tick.magna.data.domain.Legislatura
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.home_legislatura_period
import org.jetbrains.compose.resources.stringResource

/**
 * Years only. The exact days a term opens and closes are noise next to the question the
 * selector answers, and a date that does not look like an ISO one is shown as it arrived
 * rather than being cut to four characters that mean nothing.
 *
 * Out of the sheet and into its own file because the Home's header shows the same thing now:
 * the two would drift, and they are the same sentence about the same term.
 */
@Composable
internal fun Legislatura.periodo(): String =
    stringResource(Res.string.home_legislatura_period, startDate.ano(), endDate.ano())

internal fun String.ano(): String =
    if (length >= 4 && take(4).all { it.isDigit() }) take(4) else this
