package com.tick.magna.features.deputados.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.usecases.DeputadosListState
import com.tick.magna.data.usecases.GetDeputadosListUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DeputadosSearchViewModel(
    private val dispatcher: DispatcherInterface,
    getDeputadosList: GetDeputadosListUseCase
): ViewModel() {

    private val _state = MutableStateFlow(DeputadosSearchState())
    val state = _state.asStateFlow()

    fun processAction(action: DeputadosSearchAction) {
        when (action) {
            is DeputadosSearchAction.SetFilter -> handleFilter(action.filter)
        }
    }

    init {
        viewModelScope.launch(dispatcher.io) {
            getDeputadosList().collect {
                when (it) {
                    DeputadosListState.Loading -> _state.update { state -> state.copy(isLoading = true) }
                    is DeputadosListState.Success -> {
                        val deputadosUfs = it.deputados.mapNotNull { deputado -> deputado.uf }.sorted().toSet()
                        val deputadosPartidos = it.deputados.mapNotNull { deputado -> deputado.partido }.sorted().toSet()

                        _state.update { state ->
                            state.copy(
                                isLoading = false,
                                deputados = it.deputados,
                                deputadosUfs = deputadosUfs,
                                deputadoPartidos = deputadosPartidos
                            )
                        }
                    }
                    DeputadosListState.Error -> _state.update { state -> state.copy(isError = true) }
                }
            }
        }
    }

    private fun handleFilter(filter: Filter) {
        viewModelScope.launch(dispatcher.default) {
            val updatedFilter = _state.value.filters + filter
            val currentDeputados = _state.value.deputados

            val filteredDeputados = currentDeputados.filter { deputado ->
                updatedFilter.forEach { it.filter(deputado) }
            }

            _state.update { it.copy(deputadosSearch = filteredDeputados) }
        }
    }
}