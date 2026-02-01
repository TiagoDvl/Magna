package com.tick.magna.features.deputados.recent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.repository.deputados.DeputadosRepositoryInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecentDeputadosViewModel(
    private val deputadosRepository: DeputadosRepositoryInterface,
    dispatcherInterface: DispatcherInterface
): ViewModel() {

    private val _recentDeputadosState = MutableStateFlow<RecentDeputadosState>(RecentDeputadosState.Empty)
    val recentDeputadosState: StateFlow<RecentDeputadosState> = _recentDeputadosState.asStateFlow()

    init {
        viewModelScope.launch(dispatcherInterface.io) {
            deputadosRepository.getDeputados().collect { recentDeputados ->
                val state = when {
                    recentDeputados.isEmpty() -> RecentDeputadosState.Empty
                    else -> RecentDeputadosState.Peak(recentDeputados.take(MAX_RECENT_DEPUTADOS))
                }
                _recentDeputadosState.value = state
            }
        }
    }

    companion object {

        private const val MAX_RECENT_DEPUTADOS = 10
    }
}