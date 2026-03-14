package com.tick.magna.features.partidos.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.domain.DeputadoMembro
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.PartidosRepositoryInterface
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
) : ViewModel() {

    companion object {
        private const val TAG = "PartidoDetailsViewModel"
        private val AGE_LABELS = listOf("<30", "30-39", "40-49", "50-59", "60-69", "70+")
        private const val CURRENT_YEAR = 2026
    }

    private val partidoId: String = savedStateHandle.toRoute<PartidoDetailsArgs>().partidoId

    private val _state = MutableStateFlow(PartidoDetailsState())
    val state: StateFlow<PartidoDetailsState> = _state.asStateFlow()

    init {
        viewModelScope.launch(dispatcher.io) {
            partidosRepository.getPartidoDetails(partidoId).collect { result ->
                _state.update { current ->
                    current.copy(
                        headerState = when {
                            result.hasError -> PartidoHeaderState.Error
                            result.isLoadingDetail -> PartidoHeaderState.Loading
                            result.detail != null -> PartidoHeaderState.Content(result.detail)
                            else -> PartidoHeaderState.Error
                        },
                        membersState = when {
                            result.isLoadingMembers -> PartidoMembersState.Loading
                            result.members.isEmpty() -> PartidoMembersState.Empty
                            else -> PartidoMembersState.Content(
                                members = result.members,
                                isLoadingDetails = result.isLoadingMemberDetails,
                                stats = computeStats(result.members),
                            )
                        },
                    )
                }
            }
        }
    }

    fun processAction(action: PartidoDetailsAction) {
        when (action) {
            is PartidoDetailsAction.SelectChart -> _state.update { it.copy(selectedChart = action.type) }
        }
    }

    private fun computeStats(members: List<DeputadoMembro>): PartidoStats {
        val maleCount = members.count { it.sexo == "M" }
        val femaleCount = members.count { it.sexo == "F" }

        val ageGroups = members
            .mapNotNull { it.dataNascimento?.take(4)?.toIntOrNull() }
            .filter { it > 1900 }
            .map { birthYear -> CURRENT_YEAR - birthYear }
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
