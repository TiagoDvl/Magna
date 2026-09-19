package com.tick.magna.data.source.remote.response

import com.tick.magna.data.source.remote.dto.LinkDto
import com.tick.magna.data.source.remote.dto.MembroOrgaoDto
import kotlinx.serialization.Serializable

@Serializable
data class MembrosOrgaoResponse(
    val dados: List<MembroOrgaoDto>,
    val links: List<LinkDto> = emptyList(),
)
