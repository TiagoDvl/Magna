package com.tick.magna.data.source.local

import com.tick.magna.data.santinho.CofreLocal
import com.tick.magna.data.santinho.CofreLocalInterface

import com.tick.magna.data.config.AppBuildConfig
import kotlin.experimental.ExperimentalNativeApi
import org.koin.dsl.module

@OptIn(ExperimentalNativeApi::class)
actual val platformModule = module {
    single<CofreLocalInterface> { CofreLocal() }

    single<DatabaseDriverFactory> { DatabaseDriverFactory() }

    single<AppBuildConfig> { AppBuildConfig(isDebug = kotlin.native.Platform.isDebugBinary) }
}
