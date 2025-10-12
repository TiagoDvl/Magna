package com.tick.magna

import android.app.Application
import com.tick.magna.di.appModules
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MagnaApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        Napier.base(DebugAntilog())

        startKoin {
            printLogger(level = Level.DEBUG)
            androidLogger()
            androidContext(this@MagnaApplication)
            modules(appModules)
        }
    }
}