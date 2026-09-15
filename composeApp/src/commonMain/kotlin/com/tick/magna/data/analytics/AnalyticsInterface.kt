package com.tick.magna.data.analytics

interface AnalyticsInterface {

    fun track(event: AnalyticsEvent)

    fun setUserProperty(key: String, value: String?)
}
