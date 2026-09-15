package com.tick.magna.data.source.remote

import com.tick.magna.data.analytics.AnalyticsEvent
import com.tick.magna.data.analytics.AnalyticsInterface
import com.tick.magna.data.analytics.toEndpointName
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object HttpClientFactory {

    fun create(isDebug: Boolean, analytics: AnalyticsInterface): HttpClient {
        return HttpClient {
            install(ContentNegotiation) {
                json(
                    Json {
                        prettyPrint = isDebug
                        isLenient = true
                        ignoreUnknownKeys = true
                    }
                )
            }

            if (isDebug) {
                install(Logging) {
                    logger = Logger.DEFAULT
                    level = LogLevel.INFO
                }
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
}
