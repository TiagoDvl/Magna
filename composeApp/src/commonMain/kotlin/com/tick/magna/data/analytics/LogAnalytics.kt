package com.tick.magna.data.analytics

import com.tick.magna.data.logger.AppLoggerInterface

/**
 * Default tracker. Writes events to the app log instead of sending them anywhere.
 *
 * This is what iOS and Desktop use, since Magna only ships Firebase on Android. Android
 * replaces it at startup with the Firebase implementation.
 */
class LogAnalytics(private val logger: AppLoggerInterface) : AnalyticsInterface {

    override fun track(event: AnalyticsEvent) {
        val params = event.params.entries.joinToString { (key, value) -> "$key=$value" }
        logger.d(if (params.isEmpty()) event.name else "${event.name} { $params }", TAG)
    }

    override fun setUserProperty(key: String, value: String?) {
        logger.d("userProperty $key=$value", TAG)
    }

    private companion object {
        const val TAG = "Analytics"
    }
}
