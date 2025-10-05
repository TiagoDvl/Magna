package com.tick.magna.ui.utils

import androidx.compose.runtime.Composable
import com.tick.magna.data.di.appModules
import org.koin.compose.KoinApplication

@Composable
fun PreviewWrapper(
    content: @Composable () -> Unit
) {
    KoinApplication(
        application = {
            modules(appModules)
        }
    ) {
        content()
    }
}