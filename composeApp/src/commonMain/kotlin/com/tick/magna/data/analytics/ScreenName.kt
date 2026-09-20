package com.tick.magna.data.analytics

/**
 * Turns a type-safe navigation route into a short, stable screen name.
 *
 * Routes arrive fully qualified and carry argument placeholders, for example
 * `com.tick.magna.features.deputados.details.DeputadoDetailsArgs/{deputadoId}`. Only the
 * placeholder is present, never a real id, so nothing identifying reaches analytics.
 */
internal fun String.toScreenName(): String {
    val withoutArguments = substringBefore('/').substringBefore('?')
    val simpleName = withoutArguments.substringAfterLast('.')
    return simpleName.removeSuffix(ARGS_SUFFIX).ifEmpty { simpleName }
}

/**
 * Whether opening this screen is worth counting.
 *
 * One screen is not, and it is not a matter of taste. The santinho holds the numbers somebody
 * means to vote for; "this person opened their santinho" is already a fact about their
 * intention to vote, and the only useful number of such facts to collect is none. The app
 * promises on that screen that nothing about it is ever sent anywhere, and a screen-view event
 * would be the app quietly breaking its own promise one route above.
 *
 * Enforced here rather than by remembering not to add a call: the tracker is global, so the
 * exclusion has to be too.
 */
internal fun String.relatavel(): Boolean = toScreenName() !in NAO_RELATAVEIS

private const val ARGS_SUFFIX = "Args"

private val NAO_RELATAVEIS = setOf("Santinho")
