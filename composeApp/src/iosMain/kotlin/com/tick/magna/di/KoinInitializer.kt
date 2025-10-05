package com.tick.magna.di

import com.tick.magna.data.di.appModules
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.core.context.startKoin

fun initKoin() {
    Napier.base(DebugAntilog())

    startKoin {
        modules(appModules)
    }
}