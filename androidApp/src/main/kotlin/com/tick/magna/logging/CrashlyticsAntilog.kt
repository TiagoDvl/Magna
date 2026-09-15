package com.tick.magna.logging

import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel

/**
 * Feeds Napier into Crashlytics so a crash report arrives with the trail that led to it.
 *
 * Before this, Crashlytics captured the stack trace and nothing else. Every repository in
 * the app already logs what it is doing through [com.tick.magna.data.logger.AppLoggerInterface];
 * this turns those lines into breadcrumbs.
 *
 * Verbose and debug lines are dropped: they are noisy, and Crashlytics keeps only the last
 * 64 KB of log per session.
 */
class CrashlyticsAntilog(
    private val crashlytics: FirebaseCrashlytics = FirebaseCrashlytics.getInstance(),
) : Antilog() {

    override fun performLog(
        priority: LogLevel,
        tag: String?,
        throwable: Throwable?,
        message: String?,
    ) {
        if (priority < LogLevel.INFO) return

        message?.let { crashlytics.log("${priority.name.first()}/${tag ?: "Magna"}: $it") }

        if (priority >= LogLevel.ERROR) {
            throwable?.let { crashlytics.recordException(it) }
        }
    }
}
