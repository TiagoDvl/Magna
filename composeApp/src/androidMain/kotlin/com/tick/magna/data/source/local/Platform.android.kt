package com.tick.magna.data.source.local

import android.content.Context
import android.content.pm.ApplicationInfo
import com.tick.magna.data.config.AppBuildConfig
import com.tick.magna.data.santinho.CofreLocal
import com.tick.magna.data.santinho.CofreLocalInterface
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<CofreLocalInterface> { CofreLocal() }

    single<DatabaseDriverFactory> { DatabaseDriverFactory(get()) }

    single<AppBuildConfig> {
        val context = get<Context>()
        val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        AppBuildConfig(isDebug = isDebuggable)
    }
}
