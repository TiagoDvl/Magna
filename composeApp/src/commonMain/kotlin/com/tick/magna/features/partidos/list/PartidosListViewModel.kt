package com.tick.magna.features.partidos.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.PartidosRepositoryInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PartidosListViewModel(
    private val partidosRepository: PartidosRepositoryInterface,
    private val dispatcher: DispatcherInterface,
    private val logger: AppLoggerInterface,
) : ViewModel() {

    companion object {
        private const val TAG = "PartidosListViewModel"
    }

    private val _state = MutableStateFlow(PartidosListState())
    val state: StateFlow<PartidosListState> = _state.asStateFlow()

    init {
        viewModelScope.launch(dispatcher.io) {
            try {
                partidosRepository.getPartidos().collect { partidos ->
                    _state.update {
                        PartidosListState(
                            partidos = partidos.sortedByDescending { it.totalMembros },
                            isLoading = false,
                        )
                    }
                }
            } catch (e: Exception) {
                logger.e("state: failed to load partidos", e, TAG)
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}
