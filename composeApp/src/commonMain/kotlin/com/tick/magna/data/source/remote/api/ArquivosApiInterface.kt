package com.tick.magna.data.source.remote.api

/** What a `HEAD` on an annual file says before a single byte of it is transferred. */
data class ArquivoInfo(val bytes: Long, val lastModified: String?)

interface ArquivosApiInterface {

    /**
     * The size and the publication date, without downloading.
     *
     * This is what lets the screen state the weight before asking, and what makes a refresh
     * free to offer: comparing `last-modified` says whether a newer snapshot exists at all.
     * The files carry `content-length`, `last-modified`, `etag` and `accept-ranges`.
     */
    suspend fun head(url: String): ArquivoInfo

    /**
     * Streams a file line by line, reporting how many bytes have arrived.
     *
     * Line by line because the whole point is not to hold 16 MB in memory, and because the
     * files have no field spanning two lines — measured over 24082 rows at four points.
     *
     * Cancelling the coroutine cancels the transfer; this is called from a screen the person
     * can leave.
     */
    suspend fun download(url: String, onProgress: (Long) -> Unit, onLine: suspend (String) -> Unit)
}
