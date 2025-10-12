package com.tick.magna

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.tick.magna.di.appModules
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.core.context.startKoin

fun main() = application {
    Napier.base(DebugAntilog())

    startKoin {
        modules(appModules)
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Magna",
    ) {
        App()
    }
}