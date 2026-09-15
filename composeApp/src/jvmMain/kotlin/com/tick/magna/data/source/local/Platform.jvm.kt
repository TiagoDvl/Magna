package com.tick.magna.data.source.local

import com.tick.magna.data.config.AppBuildConfig
import org.koin.dsl.module

actual val platformModule = module {
    single { DatabaseDriverFactory() }

    // The desktop target is a development surface only; it is never distributed.
    single<AppBuildConfig> { AppBuildConfig(isDebug = true) }
}
