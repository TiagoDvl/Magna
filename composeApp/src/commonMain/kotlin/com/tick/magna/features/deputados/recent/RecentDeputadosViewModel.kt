package com.tick.magna.features.deputados.recent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.deputados.DeputadosRepositoryInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecentDeputadosViewModel(
    private val deputadosRepository: DeputadosRepositoryInterface,
    dispatcherInterface: DispatcherInterface,
    private val logger: AppLoggerInterface,
): ViewModel() {

    companion object {
        private const val TAG = "RecentDeputadosViewModel"
        private const val MAX_RECENT_DEPUTADOS = 10
    }

    private val _recentDeputadosState = MutableStateFlow<RecentDeputadosState>(RecentDeputadosState.Empty)
    val recentDeputadosState: StateFlow<RecentDeputadosState> = _recentDeputadosState.asStateFlow()

    init {
        viewModelScope.launch(dispatcherInterface.io) {
            deputadosRepository.getRecentDeputados().collect { recentDeputados ->
                val state = when {
                    recentDeputados.isEmpty() -> RecentDeputadosState.Empty
                    else -> RecentDeputadosState.Peak(recentDeputados.reversed().take(MAX_RECENT_DEPUTADOS))
                }
                logger.d("recentDeputados: ${recentDeputados.size} items → $state", TAG)
                _recentDeputadosState.value = state
            }
        }
    }
}
