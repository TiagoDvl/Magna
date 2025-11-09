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
            is DeputadosSearchAction.Search -> search(action.query)
            DeputadosSearchAction.CancelSearchMode -> cancelSearch()
        }
    }

    init {
        viewModelScope.launch(dispatcher.io) {
            getDeputadosList().collect {
                when (it) {
                    DeputadosListState.Loading -> _state.update { state -> state.copy(isLoading = true) }
                    is DeputadosListState.Success -> _state.update { state -> state.copy(isLoading = false, deputados = it.deputados) }
                    DeputadosListState.Error -> _state.update { state -> state.copy(isError = true) }
                }
            }
        }
    }

    private fun search(query: String) {
        viewModelScope.launch(dispatcher.default) {
            val currentDeputados = _state.value.deputados
            val filteredDeputados = currentDeputados.filter { deputado ->
                deputado.name.contains(other = query, ignoreCase = true)
            }
            _state.update { it.copy(deputadosSearch = filteredDeputados) }
        }
    }

    private fun cancelSearch() {
        viewModelScope.launch(dispatcher.default) {
            _state.update { it.copy(deputadosSearch = null) }
        }
    }
}

sealed interface DeputadosSearchAction {

    data class Search(val query: String): DeputadosSearchAction
    data object CancelSearchMode: DeputadosSearchAction
}
