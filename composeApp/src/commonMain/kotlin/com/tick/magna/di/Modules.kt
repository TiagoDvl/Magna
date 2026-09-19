@file:OptIn(ExperimentalCoroutinesApi::class)

package com.tick.magna.di

import androidx.lifecycle.SavedStateHandle
import app.cash.sqldelight.db.SqlDriver
import com.tick.magna.DeputadoDetailsQueries
import com.tick.magna.DeputadoQueries
import com.tick.magna.LegislaturaQueries
import com.tick.magna.MagnaDatabase
import com.tick.magna.OrgaoQueries
import com.tick.magna.PartidoQueries
import com.tick.magna.ProposicaoQueries
import com.tick.magna.SiglaTipoQueries
import com.tick.magna.UserQueries
import com.tick.magna.data.analytics.AnalyticsInterface
import com.tick.magna.data.analytics.LogAnalytics
import com.tick.magna.data.config.AppBuildConfig
import com.tick.magna.data.dispatcher.AppDispatcher
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.logger.NapierLogger
import com.tick.magna.data.repository.PartidosRepository
import com.tick.magna.data.repository.PartidosRepositoryInterface
import com.tick.magna.data.repository.deputados.DeputadosRepository
import com.tick.magna.data.repository.deputados.DeputadosRepositoryInterface
import com.tick.magna.data.repository.legislaturas.LegislaturasRepository
import com.tick.magna.data.repository.legislaturas.LegislaturasRepositoryInterface
import com.tick.magna.data.repository.orgaos.OrgaosRepository
import com.tick.magna.data.repository.orgaos.OrgaosRepositoryInterface
import com.tick.magna.data.repository.proposicoes.ProposicoesRepository
import com.tick.magna.data.repository.proposicoes.ProposicoesRepositoryInterface
import com.tick.magna.data.repository.user.UserRepository
import com.tick.magna.data.repository.user.UserRepositoryInterface
import com.tick.magna.data.source.local.DatabaseDriverFactory
import com.tick.magna.data.source.local.dao.DeputadoDao
import com.tick.magna.data.source.local.dao.DeputadoDaoInterface
import com.tick.magna.data.source.local.dao.DeputadoDetailsDao
import com.tick.magna.data.source.local.dao.DeputadoDetailsDaoInterface
import com.tick.magna.data.source.local.dao.DeputadoExpenseDao
import com.tick.magna.data.source.local.dao.DeputadoExpenseDaoInterface
import com.tick.magna.data.source.local.dao.LegislaturaDao
import com.tick.magna.data.source.local.dao.LegislaturaDaoInterface
import com.tick.magna.data.source.local.dao.OrgaoDao
import com.tick.magna.data.source.local.dao.OrgaoDaoInterface
import com.tick.magna.data.source.local.dao.PartidoDao
import com.tick.magna.data.source.local.dao.PartidoDaoInterface
import com.tick.magna.data.source.local.dao.ProposicaoDao
import com.tick.magna.data.source.local.dao.ProposicaoDaoInterface
import com.tick.magna.data.source.local.dao.SiglaTipoDao
import com.tick.magna.data.source.local.dao.SiglaTipoDaoInterface
import com.tick.magna.data.source.local.dao.UserDao
import com.tick.magna.data.source.local.dao.UserDaoInterface
import com.tick.magna.data.source.local.platformModule
import com.tick.magna.data.source.remote.HttpClientFactory
import com.tick.magna.data.source.remote.api.DeputadosApi
import com.tick.magna.data.source.remote.api.DeputadosApiInterface
import com.tick.magna.data.source.remote.api.LegislaturasApi
import com.tick.magna.data.source.remote.api.LegislaturasApiInterface
import com.tick.magna.data.source.remote.api.OrgaosApi
import com.tick.magna.data.source.remote.api.OrgaosApiInterface
import com.tick.magna.data.source.remote.api.PartidosApi
import com.tick.magna.data.source.remote.api.PartidosApiInterface
import com.tick.magna.data.source.remote.api.ProposicoesApi
import com.tick.magna.data.source.remote.api.ProposicoesApiInterface
import com.tick.magna.data.source.remote.api.VotacoesApi
import com.tick.magna.data.source.remote.api.VotacoesApiInterface
import com.tick.magna.data.usecases.SyncUserInformationUseCase
import com.tick.magna.features.comissoes.permanentes.component.ComissoesPermanentesViewModel
import com.tick.magna.features.comissoes.permanentes.detail.ComissaoPermanenteDetailViewModel
import com.tick.magna.features.deputados.details.DeputadoDetailsViewModel
import com.tick.magna.features.deputados.recent.RecentDeputadosViewModel
import com.tick.magna.features.deputados.search.DeputadosSearchViewModel
import com.tick.magna.features.home.HomeViewModel
import com.tick.magna.features.partidos.component.PartidosComponentViewModel
import com.tick.magna.features.partidos.details.PartidoDetailsViewModel
import com.tick.magna.features.partidos.list.PartidosListViewModel
import com.tick.magna.features.proposicoes.component.RecentProposicoesViewModel
import com.tick.magna.features.proposicoes.details.ProposicaoDetailsViewModel
import io.ktor.client.HttpClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val databaseModule = module {
    single<SqlDriver> { get<DatabaseDriverFactory>().createDriver() }

    single<MagnaDatabase> { MagnaDatabase(get()) }

    single<UserQueries> { get<MagnaDatabase>().userQueries }
    single<DeputadoQueries> { get<MagnaDatabase>().deputadoQueries }
    single<DeputadoDetailsQueries> { get<MagnaDatabase>().deputadoDetailsQueries }
    single<PartidoQueries> { get<MagnaDatabase>().partidoQueries }
    single<SiglaTipoQueries> { get<MagnaDatabase>().siglaTipoQueries }
    single<ProposicaoQueries> { get<MagnaDatabase>().proposicaoQueries }
    single<OrgaoQueries> { get<MagnaDatabase>().orgaoQueries }
    single<LegislaturaQueries> { get<MagnaDatabase>().legislaturaQueries }

    single<UserDaoInterface> { UserDao(get(), get()) }
    single<DeputadoDaoInterface> { DeputadoDao(get(), get(), get()) }
    single<DeputadoDetailsDaoInterface> { DeputadoDetailsDao(get(), get(), get()) }
    single<PartidoDaoInterface> { PartidoDao(get(), get(), get()) }
    single<OrgaoDaoInterface> { OrgaoDao(get(), get()) }
    single<DeputadoExpenseDaoInterface> { DeputadoExpenseDao(get(), get()) }
    single<SiglaTipoDaoInterface> { SiglaTipoDao(get(), get()) }
    single<ProposicaoDaoInterface> { ProposicaoDao(get(), get()) }
    single<LegislaturaDaoInterface> { LegislaturaDao(get(), get(), get()) }
}

val dataModule = module {
    // Dispatcher
    single<DispatcherInterface> { AppDispatcher() }

    // Http
    single<HttpClient> { HttpClientFactory.create(isDebug = get<AppBuildConfig>().isDebug, analytics = get()) }

    // Api
    single<DeputadosApiInterface> { DeputadosApi(get()) }
    single<PartidosApiInterface> { PartidosApi(get()) }
    single<ProposicoesApiInterface> { ProposicoesApi(get()) }
    single<OrgaosApiInterface> { OrgaosApi(get()) }
    single<VotacoesApiInterface> { VotacoesApi(get()) }
    single<LegislaturasApiInterface> { LegislaturasApi(get()) }

    // Repositories
    single<DeputadosRepositoryInterface> {
        DeputadosRepository(get(), get(), get(), get(), get(), get())
    }
    single<PartidosRepositoryInterface> { PartidosRepository(get(), get(), get(), get(), get()) }
    single<ProposicoesRepositoryInterface> { ProposicoesRepository(get(), get(), get(), get(), get()) }
    single<OrgaosRepositoryInterface> { OrgaosRepository(get(), get(), get(), get()) }
    single<UserRepositoryInterface> { UserRepository(get(), get()) }
    single<LegislaturasRepositoryInterface> { LegislaturasRepository(get(), get(), get()) }
}

val useCaseModule = module {
    factoryOf(::SyncUserInformationUseCase)
}

val loggingModule = module {
    single<AppLoggerInterface> { NapierLogger() }

    // Default tracker. Android replaces this at startup with the Firebase one.
    single<AnalyticsInterface> { LogAnalytics(get()) }
}

val viewModelModule = module {
    viewModel { HomeViewModel(get(), get(), get(), get(), get()) }
    viewModel { RecentDeputadosViewModel(get(), get(), get(), get()) }
    viewModel { DeputadosSearchViewModel(get(), get(), get(), get()) }
    viewModel { (handle: SavedStateHandle) -> DeputadoDetailsViewModel(handle, get(), get(), get(), get()) }
    viewModel { RecentProposicoesViewModel(get(), get(), get(), get()) }
    viewModel { ComissoesPermanentesViewModel(get(), get(), get()) }
    viewModel { (handle: SavedStateHandle) -> ComissaoPermanenteDetailViewModel(handle, get(), get(), get(), get()) }
    viewModel { PartidosComponentViewModel(get(), get(), get(), get()) }
    viewModel { PartidosListViewModel(get(), get(), get(), get()) }
    viewModel { (handle: SavedStateHandle) -> PartidoDetailsViewModel(handle, get(), get(), get(), get()) }
    viewModel { (handle: SavedStateHandle) -> ProposicaoDetailsViewModel(handle, get(), get(), get(), get()) }
}

val appModules = listOf(
    platformModule,
    databaseModule,
    dataModule,
    useCaseModule,
    loggingModule,
    viewModelModule,
)