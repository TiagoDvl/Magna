package com.tick.magna.data.config

/**
 * Build facts the shared code needs but cannot read on its own.
 *
 * [isDebug] gates verbose logging. Release builds must not log every HTTP request and every
 * Koin lookup: it costs CPU, fills the device log with the user's browsing, and buries the
 * Crashlytics breadcrumbs that matter.
 */
data class AppBuildConfig(val isDebug: Boolean)
