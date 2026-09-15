package com.tick.magna.di

import com.tick.magna.data.analytics.AnalyticsEvent
import com.tick.magna.data.analytics.AnalyticsInterface
import kotlin.test.Test
import kotlin.test.assertEquals
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/**
 * MagnaApplication starts Koin with `appModules + androidAnalyticsModule`, relying on the
 * later module replacing the shared LogAnalytics binding. If Koin refused that override it
 * would throw at startup, crashing the app on launch for every user, so the assumption is
 * pinned here rather than trusted.
 */
class AnalyticsOverrideTest {

    private class RecordingAnalytics(val id: String) : AnalyticsInterface {
        override fun track(event: AnalyticsEvent) = Unit
        override fun setUserProperty(key: String, value: String?) = Unit
    }

    @Test
    fun a_later_module_replaces_an_earlier_binding_of_the_same_type() {
        val shared = module { single<AnalyticsInterface> { RecordingAnalytics("shared") } }
        val platform = module { single<AnalyticsInterface> { RecordingAnalytics("platform") } }

        val koinApplication = koinApplication { modules(listOf(shared) + platform) }

        val resolved = koinApplication.koin.get<AnalyticsInterface>() as RecordingAnalytics
        assertEquals("platform", resolved.id)

        koinApplication.close()
    }

    @Test
    fun the_shared_binding_is_used_when_no_platform_module_is_added() {
        val shared = module { single<AnalyticsInterface> { RecordingAnalytics("shared") } }

        val koinApplication = koinApplication { modules(listOf(shared)) }

        val resolved = koinApplication.koin.get<AnalyticsInterface>() as RecordingAnalytics
        assertEquals("shared", resolved.id)

        koinApplication.close()
    }
}
