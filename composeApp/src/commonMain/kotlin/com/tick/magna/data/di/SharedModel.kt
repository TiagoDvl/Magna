package com.tick.magna.data.di

import com.tick.magna.data.dispatcher.AppDispatcher
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.logger.NapierLogger
import com.tick.magna.data.repository.DeputadosRepository
import com.tick.magna.data.repository.DeputadosRepositoryInterface
import com.tick.magna.data.repository.PartidosRepository
import com.tick.magna.data.repository.PartidosRepositoryInterface
import com.tick.magna.data.usecases.GetDeputadosListUseCase
import com.tick.magna.data.source.remote.HttpClientFactory
import com.tick.magna.data.source.remote.api.DeputadosApi
import com.tick.magna.data.source.remote.api.DeputadosApiInterface
import com.tick.magna.data.source.remote.api.LegislaturaApi
import com.tick.magna.data.source.remote.api.LegislaturaApiInterface
import com.tick.magna.data.source.remote.api.PartidoApi
import com.tick.magna.data.source.remote.api.PartidoApiInterface
import com.tick.magna.data.usecases.GetPartidosListUseCase
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
    single<PartidoApiInterface> { PartidoApi(get()) }

    // Repositories
    single<DeputadosRepositoryInterface> { DeputadosRepository(get(), get(), get()) }
    single<PartidosRepositoryInterface> { PartidosRepository(get(), get(), get()) }
}

val useCaseModule = module {
    single { GetDeputadosListUseCase(get(), get()) }
    single { GetPartidosListUseCase(get(), get()) }
}

val loggingModule = module {
    single<AppLoggerInterface> { NapierLogger() }
}

val viewModelModule = module {
    viewModel { HomeViewModel(get(), get(), get()) }
}

val appModules = listOf(
    dataModule,
    useCaseModule,
    loggingModule,
    viewModelModule,
)