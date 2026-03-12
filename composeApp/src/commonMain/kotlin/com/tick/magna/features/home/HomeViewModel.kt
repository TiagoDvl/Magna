package com.tick.magna.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.deputados.DeputadosRepositoryInterface
import com.tick.magna.data.usecases.SyncUserInformationUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val dispatcher: DispatcherInterface,
    private val syncUserInformation: SyncUserInformationUseCase,
    private val deputadosRepository: DeputadosRepositoryInterface,
    private val logger: AppLoggerInterface
): ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val _homeState = MutableStateFlow(HomeState())
    val homeState: StateFlow<HomeState> = _homeState.asStateFlow()

    private var syncJob: Job? = null
    private var searchJob: Job? = null

    init {
        trySync()
    }

    fun processAction(action: HomeAction) {
        logger.d("processAction: $action", TAG)
        when (action) {
            HomeAction.RetrySync -> trySync()
            is HomeAction.SearchDeputado -> handleSearchQuery(action.query)
        }
    }

    private fun handleSearchQuery(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch(dispatcher.io) {
            deputadosRepository.getDeputados(query).collect { results ->
                logger.d("Search '$query' → ${results.size} results", TAG)
                _homeState.update { it.copy(filteredDeputados = results) }
            }
        }
    }

    fun trySync() {
        logger.d("trySync", TAG)
        syncJob?.cancel()
        syncJob = viewModelScope.launch(dispatcher.io) {
            syncUserInformation().collect { state ->
                logger.d("syncState → $state", TAG)
                _homeState.update { it.copy(syncState = state) }
            }
        }
    }
}
