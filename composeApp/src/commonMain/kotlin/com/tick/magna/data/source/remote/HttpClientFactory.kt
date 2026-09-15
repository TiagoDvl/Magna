package com.tick.magna.data.source.remote

import com.tick.magna.data.analytics.AnalyticsEvent
import com.tick.magna.data.analytics.AnalyticsInterface
import com.tick.magna.data.analytics.toEndpointName
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json

/**
 * The Camara gateway answers 403 to any request carrying an Accept-Charset header, whatever
 * the value: UTF-8, ISO-8859-1, a wildcard, even an empty one. The body is an HTML page
 * saying the security system blocked the operation, so the failure looks like a server
 * outage rather than a rejected header.
 *
 * Ktor installs HttpPlainText by default and it stamps that header on every request, which
 * meant every call the app made was refused.
 *
 * HttpPlainText adds the header in the request pipeline's Render phase, which is why
 * removing it through defaultRequest does nothing: that runs earlier and the header is put
 * back afterwards. The send pipeline runs after Render and before the builder is turned
 * into engine request data, so this is the first point where the header both exists and can
 * still be dropped.
 */
private val StripAcceptCharset = createClientPlugin("StripAcceptCharset") {
    on(Send) { request ->
        request.headers.remove(HttpHeaders.AcceptCharset)
        proceed(request)
    }
}

object HttpClientFactory {

    fun create(isDebug: Boolean, analytics: AnalyticsInterface): HttpClient {
        return HttpClient {
            install(StripAcceptCharset)

            install(ContentNegotiation) {
                json(apiJson(prettyPrint = isDebug))
            }

            if (isDebug) {
                install(Logging) {
                    logger = Logger.DEFAULT
                    level = LogLevel.INFO
                }
            }

            // Without a timeout a stalled request never returns, and the first-run sync
            // dialog has no way out: the user sits on a spinner until they kill the app.
            install(HttpTimeout) {
                requestTimeoutMillis = REQUEST_TIMEOUT_MS
                connectTimeoutMillis = CONNECT_TIMEOUT_MS
                socketTimeoutMillis = SOCKET_TIMEOUT_MS
            }

            // Server errors only. A timeout is deliberately not retried: three attempts at
            // thirty seconds would leave someone staring at a spinner for a minute and a
            // half before being told it failed.
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = MAX_RETRIES)
                exponentialDelay()
            }

            expectSuccess = true

            // Every API failure is reported from here rather than from the fifteen catch
            // blocks spread across the repositories. This also catches the failures that
            // never produce a response at all, such as a timeout or no connectivity,
            // which is the case that matters most for an app used offline.
            HttpResponseValidator {
                handleResponseExceptionWithRequest { exception, request ->
                    analytics.track(
                        AnalyticsEvent.ApiError(
                            endpoint = request.url.encodedPath.toEndpointName(),
                            status = (exception as? ResponseException)?.response?.status?.value,
                        )
                    )
                }
            }

            defaultRequest {
                url("https://dadosabertos.camara.leg.br/api/v2/")
            }
        }
    }

    private const val REQUEST_TIMEOUT_MS = 30_000L
    private const val CONNECT_TIMEOUT_MS = 10_000L
    private const val SOCKET_TIMEOUT_MS = 30_000L
    private const val MAX_RETRIES = 2
}
