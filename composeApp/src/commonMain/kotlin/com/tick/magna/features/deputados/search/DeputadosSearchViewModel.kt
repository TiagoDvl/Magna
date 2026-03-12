package com.tick.magna.features.deputados.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.deputados.DeputadosRepositoryInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DeputadosSearchViewModel(
    private val dispatcher: DispatcherInterface,
    private val deputadosRepository: DeputadosRepositoryInterface,
    private val logger: AppLoggerInterface,
) : ViewModel() {

    companion object {
        private const val TAG = "DeputadosSearchViewModel"
    }

    private val _state = MutableStateFlow(DeputadosSearchState())
    val state = _state.asStateFlow()

    fun processAction(action: DeputadosSearchAction) {
        logger.d("processAction: $action", TAG)
        when (action) {
            is DeputadosSearchAction.OnFilter -> handleFilter(action.filter)
        }
    }

    init {
        viewModelScope.launch(dispatcher.io) {
            deputadosRepository.getDeputados().collect { deputados ->
                val deputadosUfs = deputados.mapNotNull { it.uf }.sorted().toSet()
                val deputadosPartidos = deputados.mapNotNull { it.partido }.sorted().toSet()
                logger.d("loaded ${deputados.size} deputados, ${deputadosUfs.size} UFs, ${deputadosPartidos.size} partidos", TAG)

                _state.update { state ->
                    state.copy(
                        isLoading = false,
                        deputados = deputados,
                        deputadosUfs = deputadosUfs,
                        deputadoPartidos = deputadosPartidos
                    )
                }
            }
        }
    }

    private fun handleFilter(filter: Filter) {
        viewModelScope.launch(dispatcher.default) {
            val filters = state.value.filters

            if (filter.isRemoved) {
                filters.remove(filter.filterKey)
            } else {
                filters[filter.filterKey] = filter
            }

            val filteredDeputados = _state.value.deputados.filter { deputado ->
                filters.all { filter -> filter.value.filter(deputado) }
            }

            logger.d("handleFilter: ${filters.size} active filters → ${filteredDeputados.size} results", TAG)
            _state.update { it.copy(deputadosSearch = if (filters.isEmpty()) null else filteredDeputados) }
        }
    }
}
