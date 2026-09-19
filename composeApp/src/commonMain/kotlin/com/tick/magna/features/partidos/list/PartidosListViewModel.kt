package com.tick.magna.features.partidos.list

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PartidosListViewModel(
    private val partidosRepository: PartidosRepositoryInterface,
    private val dispatcher: DispatcherInterface,
    private val logger: AppLoggerInterface,
    private val analytics: AnalyticsInterface,
) : ViewModel() {

    companion object {
        private const val TAG = "PartidosListViewModel"
    }

    private val _state = MutableStateFlow(PartidosListState())
    val state: StateFlow<PartidosListState> = _state.asStateFlow()

    init {
        viewModelScope.launch(dispatcher.io) {
            try {
                // Ordered by the query: favourites first, then by how many deputados the
                // party had in this term.
                partidosRepository.getPartidos().collect { partidos ->
                    _state.update {
                        PartidosListState(partidos = partidos, isLoading = false)
                    }
                }
            } catch (e: Exception) {
                logger.e("state: failed to load partidos", e, TAG)
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onPartidoOpened() {
        analytics.track(AnalyticsEvent.PartidoOpened(AnalyticsEvent.Source.LIST))
    }

    /**
     * Writes only. The list is a flow over the table, so the star and the reordering both come
     * back through the query rather than from a copy held here.
     */
    fun onToggleFavorito(partido: Partido) {
        val favorito = !partido.isFavorito

        viewModelScope.launch(dispatcher.io) {
            partidosRepository.setFavorito(partido.id.toString(), favorito)
            analytics.track(
                AnalyticsEvent.PartidoFavorited(
                    favorited = favorito,
                    source = AnalyticsEvent.Source.LIST,
                )
            )
        }
    }
}
