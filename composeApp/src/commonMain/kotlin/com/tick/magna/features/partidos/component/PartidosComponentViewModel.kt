package com.tick.magna.features.partidos.component

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.domain.Partido
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.PartidosRepositoryInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PartidosComponentViewModel(
    private val partidosRepository: PartidosRepositoryInterface,
    private val dispatcher: DispatcherInterface,
    private val logger: AppLoggerInterface,
) : ViewModel() {

    companion object {
        private const val TAG = "PartidosComponentViewModel"
        private const val PREVIEW_COUNT = 8
    }

    private val _state = MutableStateFlow<List<Partido>>(emptyList())
    val state: StateFlow<List<Partido>> = _state.asStateFlow()

    init {
        viewModelScope.launch(dispatcher.io) {
            try {
                partidosRepository.getPartidos().collect { partidos ->
                    _state.value = partidos
                        .sortedByDescending { it.totalMembros }
                        .take(PREVIEW_COUNT)
                }
            } catch (e: Exception) {
                logger.e("state: failed to load partidos for component", e, TAG)
            }
        }
    }
}
