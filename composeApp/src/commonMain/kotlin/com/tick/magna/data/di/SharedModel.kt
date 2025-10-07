package com.tick.magna.data.di

import com.tick.magna.data.dispatcher.AppDispatcher
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.logger.NapierLogger
import com.tick.magna.data.repository.PlenarioRepository
import com.tick.magna.data.repository.PlenarioRepositoryInterface
import com.tick.magna.data.result.GetDeputadosListUseCase
import com.tick.magna.data.source.remote.HttpClientFactory
import com.tick.magna.data.source.remote.api.DeputadosApi
import com.tick.magna.data.source.remote.api.DeputadosApiInterface
import com.tick.magna.data.source.remote.api.LegislaturaApi
import com.tick.magna.data.source.remote.api.LegislaturaApiInterface
import com.tick.magna.features.home.HomeViewModel
import io.ktor.client.HttpClient
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val dataModule = module {
    // Dispatcher
    single<DispatcherInterface> { AppDispatcher() }

    // Http
    single<HttpClient> { HttpClientFactory.create() }

    // Api
    single<DeputadosApiInterface> { DeputadosApi(get()) }
    single<LegislaturaApiInterface> { LegislaturaApi(get()) }

    // Repositories
    single<PlenarioRepositoryInterface> { PlenarioRepository(get(), get(), get()) }
}

val useCaseModule = module {
    single { GetDeputadosListUseCase(get(), get()) }
}

val loggingModule = module {
    single<AppLoggerInterface> { NapierLogger() }
}

val viewModelModule = module {
    viewModel { HomeViewModel(get(), get()) }
}

val appModules = listOf(
    dataModule,
    useCaseModule,
    loggingModule,
    viewModelModule,
)