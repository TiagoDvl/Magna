package com.tick.magna.data.source.remote.response

import com.tick.magna.data.source.remote.dto.LinkDto

/**
 * Whether the Camara says there is another page.
 *
 * Every list response carries `links`, and a `next` entry is present exactly while more
 * records exist. Reading that is safer than comparing the page size to what was asked for:
 * the endpoints do not agree on a default page size, and a caller that guesses wrong either
 * stops early or asks for a page that is not there.
 */
internal fun List<LinkDto>.hasNextPage(): Boolean {
    return any { link -> link.rel.equals("next", ignoreCase = true) }
}

/**
 * How many records the whole query has, without downloading them.
 *
 * Asked with `itens=1`, the `last` link points at the final page, and its page number is
 * therefore the record count. It is the cheapest way to measure volume the API offers, and the
 * only one: no endpoint returns a total.
 *
 * Null when there is no `last` link, which the Camara omits when everything already fits in
 * the page it just sent.
 */
internal fun List<LinkDto>.totalFromLastPage(): Int? {
    val last = firstOrNull { link -> link.rel.equals("last", ignoreCase = true) } ?: return null

    return last.href
        .substringAfter("pagina=", missingDelimiterValue = "")
        .substringBefore('&')
        .toIntOrNull()
}
