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
