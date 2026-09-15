package com.tick.magna.data.analytics

/**
 * Reduces a request path to the endpoint shape, for use as an analytics dimension.
 *
 * Two reasons this is not the raw path. The raw path carries record ids
 * (`/api/v2/deputados/204528/despesas`), which says which politician a person was reading
 * about; and every id would become its own dimension value, so the report would be
 * thousands of rows deep and say nothing.
 *
 * Any segment containing a digit is treated as an id and masked. No endpoint name in the
 * Camara API contains a digit, so the rule is safe, and it also covers ids that are not
 * purely numeric such as the `2358471-1` used for votações.
 */
internal fun String.toEndpointName(): String {
    val path = substringBefore('?').trim('/').removePrefix(API_PREFIX).trim('/')
    if (path.isEmpty()) return UNKNOWN

    return path
        .split('/')
        .joinToString("/") { segment ->
            if (segment.any { it.isDigit() }) ID_PLACEHOLDER else segment
        }
}

private const val API_PREFIX = "api/v2"
private const val ID_PLACEHOLDER = "{id}"
private const val UNKNOWN = "unknown"
