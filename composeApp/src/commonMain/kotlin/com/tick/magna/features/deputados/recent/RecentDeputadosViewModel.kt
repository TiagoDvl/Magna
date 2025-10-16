package com.tick.magna.features.deputados.recent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.usecases.GetRecentDeputadosUseCase
import com.tick.magna.data.usecases.RecentDeputadosState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecentDeputadosViewModel(
    private val getRecentDeputados: GetRecentDeputadosUseCase,
    private val dispatcherInterface: DispatcherInterface
): ViewModel() {

    private val _recentDeputadosState = MutableStateFlow<RecentDeputadosState>(RecentDeputadosState.Empty)
    val recentDeputadosState: StateFlow<RecentDeputadosState> = _recentDeputadosState.asStateFlow()

    init {
        viewModelScope.launch(dispatcherInterface.io) {
            getRecentDeputados().collect { state ->
                _recentDeputadosState.value = state
            }
        }
    }
}