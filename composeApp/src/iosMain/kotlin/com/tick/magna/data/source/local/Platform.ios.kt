package com.tick.magna.data.source.local

import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule = module {
    single { DatabaseDriverFactory() }
}