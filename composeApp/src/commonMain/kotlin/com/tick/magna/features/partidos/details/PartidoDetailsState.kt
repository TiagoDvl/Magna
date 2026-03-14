package com.tick.magna.features.partidos.details

import com.tick.magna.data.domain.DeputadoMembro
import com.tick.magna.data.domain.PartidoDetail

data class PartidoDetailsState(
    val headerState: PartidoHeaderState = PartidoHeaderState.Loading,
    val membersState: PartidoMembersState = PartidoMembersState.Loading,
    val selectedChart: PartidoChartType = PartidoChartType.GENDER,
)

sealed interface PartidoHeaderState {
    data object Loading : PartidoHeaderState
    data object Error : PartidoHeaderState
    data class Content(val detail: PartidoDetail) : PartidoHeaderState
}

sealed interface PartidoMembersState {
    data object Loading : PartidoMembersState
    data object Empty : PartidoMembersState
    data class Content(
        val members: List<DeputadoMembro>,
        val isLoadingDetails: Boolean,
        val stats: PartidoStats,
    ) : PartidoMembersState
}

data class PartidoStats(
    val maleCount: Int,
    val femaleCount: Int,
    val ageGroups: List<Pair<String, Int>>,
    val birthStateGroups: List<Pair<String, Int>>,
    val membersByRepresentingUf: Map<String, List<DeputadoMembro>>,
)

enum class PartidoChartType { GENDER, AGE, BIRTH_STATE }

sealed interface PartidoDetailsAction {
    data class SelectChart(val type: PartidoChartType) : PartidoDetailsAction
}
