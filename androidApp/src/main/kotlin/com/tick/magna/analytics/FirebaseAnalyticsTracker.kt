package com.tick.magna.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.tick.magna.data.analytics.AnalyticsEvent
import com.tick.magna.data.analytics.AnalyticsInterface
import com.tick.magna.data.logger.AppLoggerInterface

/**
 * Android tracker. Firebase only ships on Android, which is why this lives in :androidApp
 * next to google-services.json rather than in the shared module.
 */
class FirebaseAnalyticsTracker(
    context: Context,
    private val logger: AppLoggerInterface,
) : AnalyticsInterface {

    private val firebaseAnalytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(context)

    override fun track(event: AnalyticsEvent) {
        firebaseAnalytics.logEvent(event.name, event.params.toBundle())
    }

    override fun setUserProperty(key: String, value: String?) {
        firebaseAnalytics.setUserProperty(key, value)
    }

    private fun Map<String, Any>.toBundle(): Bundle = Bundle().apply {
        forEach { (key, value) ->
            when (value) {
                is String -> putString(key, value)
                is Int -> putLong(key, value.toLong())
                is Long -> putLong(key, value)
                is Float -> putDouble(key, value.toDouble())
                is Double -> putDouble(key, value)
                is Boolean -> putLong(key, if (value) 1L else 0L)
                else -> logger.w("dropped param $key: unsupported type", TAG)
            }
        }
    }

    private companion object {
        const val TAG = "Analytics"
    }
}
