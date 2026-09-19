package com.tick.magna.features.partidos.component

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.analytics.AnalyticsEvent
import com.tick.magna.data.analytics.AnalyticsInterface
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
    private val analytics: AnalyticsInterface,
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
                // No sorting here any more. The repository answers favourites first and then
                // by size; this used to sort by a column the sync never fills, so it changed
                // nothing and the carousel showed the first eight parties alphabetically.
                partidosRepository.getPartidos().collect { partidos ->
                    _state.value = partidos.take(PREVIEW_COUNT)
                }
            } catch (e: Exception) {
                logger.e("state: failed to load partidos for component", e, TAG)
            }
        }
    }

    fun onPartidoOpened() {
        analytics.track(AnalyticsEvent.PartidoOpened(AnalyticsEvent.Source.HOME_SECTION))
    }
}
