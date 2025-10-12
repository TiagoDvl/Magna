package com.tick.magna.di

import app.cash.sqldelight.db.SqlDriver
import com.tick.magna.LegislaturaQueries
import com.tick.magna.MagnaDatabase
import com.tick.magna.UserQueries
import com.tick.magna.data.dispatcher.AppDispatcher
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.logger.NapierLogger
import com.tick.magna.data.repository.DeputadosRepository
import com.tick.magna.data.repository.DeputadosRepositoryInterface
import com.tick.magna.data.repository.PartidosRepository
import com.tick.magna.data.repository.PartidosRepositoryInterface
import com.tick.magna.data.source.local.DatabaseDriverFactory
import com.tick.magna.data.source.local.dao.LegislaturaDao
import com.tick.magna.data.source.local.dao.LegislaturaDaoInterface
import com.tick.magna.data.source.local.dao.UserDao
import com.tick.magna.data.source.local.dao.UserDaoInterface
import com.tick.magna.data.source.local.platformModule
import com.tick.magna.data.usecases.GetDeputadosListUseCase
import com.tick.magna.data.source.remote.HttpClientFactory
import com.tick.magna.data.source.remote.api.DeputadosApi
import com.tick.magna.data.source.remote.api.DeputadosApiInterface
import com.tick.magna.data.source.remote.api.LegislaturaApi
import com.tick.magna.data.source.remote.api.LegislaturaApiInterface
import com.tick.magna.data.source.remote.api.PartidoApi
import com.tick.magna.data.source.remote.api.PartidoApiInterface
import com.tick.magna.data.usecases.CheckUserConfigurationUseCase
import com.tick.magna.data.usecases.GetPartidosListUseCase
import com.tick.magna.features.home.HomeViewModel
import io.ktor.client.HttpClient
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module


val databaseModule = module {
    single<SqlDriver> { get<DatabaseDriverFactory>().createDriver() }

    single<MagnaDatabase> { MagnaDatabase(get()) }

    single<UserQueries> { get<MagnaDatabase>().userQueries }
    single<LegislaturaQueries> { get<MagnaDatabase>().legislaturaQueries }

    single<UserDaoInterface> { UserDao(get(), get()) }
    single<LegislaturaDaoInterface> { LegislaturaDao(get(), get()) }
}

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
    single<DeputadosRepositoryInterface> { DeputadosRepository(get(), get()) }
    single<PartidosRepositoryInterface> { PartidosRepository(get(), get()) }
}

val useCaseModule = module {
    single { GetDeputadosListUseCase(get(), get()) }
    single { GetPartidosListUseCase(get(), get()) }
    single { CheckUserConfigurationUseCase(get(), get()) }
}

val loggingModule = module {
    single<AppLoggerInterface> { NapierLogger() }
}

val viewModelModule = module {
    viewModel { HomeViewModel(get(), get(), get(), get()) }
}

val appModules = listOf(
    platformModule,
    databaseModule,
    dataModule,
    useCaseModule,
    loggingModule,
    viewModelModule,
)