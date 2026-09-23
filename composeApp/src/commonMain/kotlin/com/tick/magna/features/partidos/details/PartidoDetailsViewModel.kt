package com.tick.magna.features.partidos.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.tick.magna.data.analytics.AnalyticsEvent
import com.tick.magna.data.analytics.AnalyticsInterface
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.domain.DeputadoMembro
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.PartidosRepositoryInterface
import com.tick.magna.data.repository.Resource
import com.tick.magna.data.repository.today
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PartidoDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val dispatcher: DispatcherInterface,
    private val partidosRepository: PartidosRepositoryInterface,
    private val logger: AppLoggerInterface,
    private val analytics: AnalyticsInterface,
) : ViewModel() {

    companion object {
        private const val TAG = "PartidoDetailsViewModel"
        private val AGE_LABELS = listOf("<30", "30-39", "40-49", "50-59", "60-69", "70+")
    }

    private val partidoId: String = savedStateHandle.toRoute<PartidoDetailsArgs>().partidoId

    private var trackedEmptyMembers = false

    private val _state = MutableStateFlow(PartidoDetailsState())
    val state: StateFlow<PartidoDetailsState> = _state.asStateFlow()

    init {
        viewModelScope.launch(dispatcher.io) {
            combine(
                partidosRepository.getPartidoDetail(partidoId),
                partidosRepository.getPartidoMembros(partidoId),
            ) { detail, membros ->
                PartidoDetailsState(
                    headerState = when (detail) {
                        Resource.Loading -> PartidoHeaderState.Loading
                        is Resource.Error -> PartidoHeaderState.Error
                        is Resource.Content -> PartidoHeaderState.Content(detail.data)
                    },
                    membersState = when (membros) {
                        Resource.Loading -> PartidoMembersState.Loading
                        is Resource.Error -> PartidoMembersState.Empty
                        is Resource.Content -> if (membros.data.isEmpty()) {
                            trackEmptyOnce(AnalyticsEvent.EmptyContent.PARTIDO_MEMBROS)
                            PartidoMembersState.Empty
                        } else {
                            PartidoMembersState.Content(
                                members = membros.data,
                                isLoadingDetails = membros.isRefreshing,
                                stats = computeStats(membros.data),
                            )
                        }
                    },
                )
            }.collect { next ->
                // selectedChart is owned by the screen,
                // not by these two requests, so the freshly built state does not get to
                // overwrite them with its defaults.
                _state.update { current ->
                    next.copy(
                        selectedChart = current.selectedChart,
                    )
                }
            }
        }
    }

    /** The result flow emits repeatedly; the empty outcome is worth reporting only once. */
    private fun trackEmptyOnce(content: AnalyticsEvent.EmptyContent) {
        if (trackedEmptyMembers) return
        trackedEmptyMembers = true
        analytics.track(AnalyticsEvent.ContentEmpty(content))
    }

    fun onMemberOpened() {
        analytics.track(AnalyticsEvent.DeputadoOpened(AnalyticsEvent.Source.MEMBROS))
    }

    fun processAction(action: PartidoDetailsAction) {
        when (action) {
            is PartidoDetailsAction.SelectChart -> {
                analytics.track(AnalyticsEvent.PartidoChartSelected(action.type.name))
                _state.update { it.copy(selectedChart = action.type) }
            }

        }
    }

    private fun computeStats(members: List<DeputadoMembro>): PartidoStats {
        // Read from the clock rather than written down. It was a constant set to 2026, which
        // would have aged every deputado by a year less than the truth from January onwards.
        val anoAtual = today().year

        val maleCount = members.count { it.sexo == "M" }
        val femaleCount = members.count { it.sexo == "F" }

        val ageGroups = members
            .mapNotNull { it.dataNascimento?.take(4)?.toIntOrNull() }
            .filter { it > 1900 }
            .map { birthYear -> anoAtual - birthYear }
            .groupBy { age ->
                when {
                    age < 30 -> "<30"
                    age < 40 -> "30-39"
                    age < 50 -> "40-49"
                    age < 60 -> "50-59"
                    age < 70 -> "60-69"
                    else -> "70+"
                }
            }
            .map { (label, ages) -> label to ages.size }
            .sortedBy { (label, _) -> AGE_LABELS.indexOf(label) }

        val birthStateGroups = members
            .mapNotNull { it.ufNascimento?.takeIf { uf -> uf.isNotBlank() } }
            .groupBy { it }
            .map { (uf, list) -> uf to list.size }
            .sortedByDescending { it.second }
            .take(10)

        val membersByUf = members
            .groupBy { it.siglaUf ?: "—" }
            .entries
            .sortedByDescending { it.value.size }
            .associate { it.key to it.value }

        return PartidoStats(
            maleCount = maleCount,
            femaleCount = femaleCount,
            ageGroups = ageGroups,
            birthStateGroups = birthStateGroups,
            membersByRepresentingUf = membersByUf,
        )
    }
}
