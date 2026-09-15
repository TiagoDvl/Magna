package com.tick.magna

import android.app.Application
import android.content.pm.ApplicationInfo
import com.tick.magna.analytics.FirebaseAnalyticsTracker
import com.tick.magna.data.analytics.AnalyticsInterface
import com.tick.magna.di.appModules
import com.tick.magna.logging.CrashlyticsAntilog
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module

class MagnaApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        val isDebug = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        // Debug builds log to Logcat. Release builds forward warnings and errors to
        // Crashlytics as breadcrumbs instead, and stay quiet in Logcat.
        Napier.base(if (isDebug) DebugAntilog() else CrashlyticsAntilog())

        startKoin {
            if (isDebug) {
                printLogger(level = Level.DEBUG)
                androidLogger()
            }
            androidContext(this@MagnaApplication)
            modules(appModules + androidAnalyticsModule)
        }
    }

    /**
     * Replaces the shared LogAnalytics binding with the Firebase one. Declared last so it
     * overrides the default from loggingModule.
     */
    private val androidAnalyticsModule = module {
        single<AnalyticsInterface> { FirebaseAnalyticsTracker(androidContext(), get()) }
    }
}
