package com.tick.magna.di

import androidx.lifecycle.SavedStateHandle
import app.cash.sqldelight.db.SqlDriver
import com.tick.magna.AppViewModel
import com.tick.magna.DeputadoDetailsQueries
import com.tick.magna.DeputadoQueries
import com.tick.magna.LegislaturaQueries
import com.tick.magna.MagnaDatabase
import com.tick.magna.PartidoQueries
import com.tick.magna.UserQueries
import com.tick.magna.data.dispatcher.AppDispatcher
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.logger.NapierLogger
import com.tick.magna.data.repository.DeputadosRepository
import com.tick.magna.data.repository.DeputadosRepositoryInterface
import com.tick.magna.data.repository.LegislaturaRepository
import com.tick.magna.data.repository.LegislaturaRepositoryInterface
import com.tick.magna.data.repository.PartidosRepository
import com.tick.magna.data.repository.PartidosRepositoryInterface
import com.tick.magna.data.source.local.DatabaseDriverFactory
import com.tick.magna.data.source.local.dao.DeputadoDao
import com.tick.magna.data.source.local.dao.DeputadoDaoInterface
import com.tick.magna.data.source.local.dao.DeputadoDetailsDao
import com.tick.magna.data.source.local.dao.DeputadoDetailsDaoInterface
import com.tick.magna.data.source.local.dao.LegislaturaDao
import com.tick.magna.data.source.local.dao.LegislaturaDaoInterface
import com.tick.magna.data.source.local.dao.PartidoDao
import com.tick.magna.data.source.local.dao.PartidoDaoInterface
import com.tick.magna.data.source.local.dao.UserDao
import com.tick.magna.data.source.local.dao.UserDaoInterface
import com.tick.magna.data.source.local.platformModule
import com.tick.magna.data.source.remote.HttpClientFactory
import com.tick.magna.data.source.remote.api.DeputadosApi
import com.tick.magna.data.source.remote.api.DeputadosApiInterface
import com.tick.magna.data.source.remote.api.LegislaturaApi
import com.tick.magna.data.source.remote.api.LegislaturaApiInterface
import com.tick.magna.data.source.remote.api.PartidosApi
import com.tick.magna.data.source.remote.api.PartidosApiInterface
import com.tick.magna.data.usecases.CheckUserConfigurationUseCase
import com.tick.magna.data.usecases.ConfigureLegislaturaUseCase
import com.tick.magna.data.usecases.GetDeputadoDetailsUseCase
import com.tick.magna.data.usecases.GetDeputadoUseCase
import com.tick.magna.data.usecases.GetDeputadosListUseCase
import com.tick.magna.data.usecases.GetRecentDeputadosUseCase
import com.tick.magna.data.usecases.SyncUserInformationUseCase
import com.tick.magna.features.deputados.detail.DeputadoDetailsViewModel
import com.tick.magna.features.deputados.recent.RecentDeputadosViewModel
import com.tick.magna.features.deputados.search.DeputadosSearchViewModel
import com.tick.magna.features.home.HomeViewModel
import com.tick.magna.features.welcome.WelcomeViewModel
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val databaseModule = module {
    single<SqlDriver> { get<DatabaseDriverFactory>().createDriver() }

    single<MagnaDatabase> { MagnaDatabase(get()) }

    single<UserQueries> { get<MagnaDatabase>().userQueries }
    single<LegislaturaQueries> { get<MagnaDatabase>().legislaturaQueries }
    single<DeputadoQueries> { get<MagnaDatabase>().deputadoQueries }
    single<DeputadoDetailsQueries> { get<MagnaDatabase>().deputadoDetailsQueries }
    single<PartidoQueries> { get<MagnaDatabase>().partidoQueries }

    single<UserDaoInterface> { UserDao(get(), get()) }
    single<LegislaturaDaoInterface> { LegislaturaDao(get(), get(), get()) }
    single<DeputadoDaoInterface> { DeputadoDao(get(), get(), get()) }
    single<DeputadoDetailsDaoInterface> { DeputadoDetailsDao(get(), get(), get()) }
    single<PartidoDaoInterface> { PartidoDao(get(), get(), get()) }
}

val dataModule = module {
    // Dispatcher
    single<DispatcherInterface> { AppDispatcher() }

    // Http
    single<HttpClient> { HttpClientFactory.create() }

    // Coroutine Scope
    factory<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.IO) }

    // Api
    single<DeputadosApiInterface> { DeputadosApi(get()) }
    single<PartidosApiInterface> { PartidosApi(get()) }
    single<LegislaturaApiInterface> { LegislaturaApi(get()) }

    // Repositories
    single<DeputadosRepositoryInterface> { DeputadosRepository(get(), get(), get(), get(), get(), get()) }
    single<PartidosRepositoryInterface> { PartidosRepository(get(), get(), get(), get(), get()) }
    single<LegislaturaRepositoryInterface> { LegislaturaRepository(get(), get()) }
}

val useCaseModule = module {
    factoryOf(::GetDeputadosListUseCase)
    factoryOf(::GetRecentDeputadosUseCase)
    factoryOf(::CheckUserConfigurationUseCase)
    factoryOf(::ConfigureLegislaturaUseCase)
    factoryOf(::GetDeputadoDetailsUseCase)
    factoryOf(::GetDeputadoUseCase)
    factoryOf(::SyncUserInformationUseCase)
}

val loggingModule = module {
    single<AppLoggerInterface> { NapierLogger() }
}

val viewModelModule = module {
    viewModel { AppViewModel(get(), get()) }
    viewModel { WelcomeViewModel(get(), get(), get()) }
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { RecentDeputadosViewModel(get(), get()) }
    viewModel { DeputadosSearchViewModel(get(), get()) }
    viewModel { (handle: SavedStateHandle) ->
        DeputadoDetailsViewModel(handle, get(), get(), get())
    }
}

val appModules = listOf(
    platformModule,
    databaseModule,
    dataModule,
    useCaseModule,
    loggingModule,
    viewModelModule,
)