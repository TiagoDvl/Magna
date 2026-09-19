package com.tick.magna.data.source.remote.api

import io.ktor.client.HttpClient
import io.ktor.client.request.head
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.readUTF8Line

class ArquivosApi(private val httpClient: HttpClient) : ArquivosApiInterface {

    override suspend fun head(url: String): ArquivoInfo {
        val response = httpClient.head(url)

        return ArquivoInfo(
            bytes = response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: 0L,
            lastModified = response.headers[HttpHeaders.LastModified],
        )
    }

    override suspend fun download(
        url: String,
        onProgress: (Long) -> Unit,
        onLine: suspend (String) -> Unit,
    ) {
        httpClient.prepareGet(url).execute { response ->
            val channel = response.bodyAsChannel()
            var bytes = 0L

            while (true) {
                val line = channel.readUTF8Line() ?: break
                onLine(line)

                // Counted from the decoded line rather than read off the channel, which does
                // not expose a running total. UTF-8 length plus the newline that was consumed,
                // so accented text does not make the bar lag behind the transfer.
                bytes += line.encodeToByteArray().size + 1
                onProgress(bytes)
            }
        }
    }
}
