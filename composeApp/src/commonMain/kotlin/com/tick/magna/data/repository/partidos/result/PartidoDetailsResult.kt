package com.tick.magna.data.repository.partidos.result

import com.tick.magna.data.domain.DeputadoMembro
import com.tick.magna.data.domain.PartidoDetail

data class PartidoDetailsResult(
    val isLoadingDetail: Boolean = true,
    val isLoadingMembers: Boolean = true,
    val isLoadingMemberDetails: Boolean = false,
    val detail: PartidoDetail? = null,
    val members: List<DeputadoMembro> = emptyList(),
    val hasError: Boolean = false,
)
