package com.tick.magna.features.comissoes.permanentes.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.analytics.AnalyticsEvent
import com.tick.magna.data.analytics.AnalyticsInterface
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.orgaos.OrgaosRepositoryInterface
import com.tick.magna.features.comissoes.permanentes.component.toComissoesPermanentes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Every permanent committee, not the ten the Home shows.
 *
 * The Home carousel is a shortlist by design; this is where the other twenty live, so that
 * ordering by activity narrows what is offered first without making anything unreachable.
 */
class ComissoesListViewModel(
    private val orgaosRepository: OrgaosRepositoryInterface,
    private val dispatcher: DispatcherInterface,
    private val logger: AppLoggerInterface,
    private val analytics: AnalyticsInterface,
) : ViewModel() {

    companion object {
        private const val TAG = "ComissoesListViewModel"
    }

    private val _state = MutableStateFlow(ComissoesListState())
    val state: StateFlow<ComissoesListState> = _state.asStateFlow()

    init {
        viewModelScope.launch(dispatcher.io) {
            try {
                orgaosRepository.getComissoesPermanentes().collect { orgaos ->
                    val comissoes = orgaos.toComissoesPermanentes()
                    logger.d("state: ${comissoes.size} comissoes", TAG)
                    _state.value = ComissoesListState(comissoes = comissoes, isLoading = false)
                }
            } catch (e: Exception) {
                logger.e("state: failed to load comissoes", e, TAG)
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun onComissaoOpened(sigla: String) {
        analytics.track(AnalyticsEvent.ComissaoOpened(sigla = sigla))
    }
}
