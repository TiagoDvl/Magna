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

private const val ARGS_SUFFIX = "Args"
