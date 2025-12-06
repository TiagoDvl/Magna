package com.tick.magna.data.source.remote.response

import com.tick.magna.data.source.remote.dto.DespesaDto
import com.tick.magna.data.source.remote.dto.LinkDto
import kotlinx.serialization.Serializable

@Serializable
data class DespesasResponse(
    val dados: List<DespesaDto>,
    val links: List<LinkDto>
)
