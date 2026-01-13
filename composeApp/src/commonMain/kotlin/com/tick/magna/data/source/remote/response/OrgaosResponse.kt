package com.tick.magna.data.source.remote.response

import com.tick.magna.data.source.remote.dto.LinkDto
import com.tick.magna.data.source.remote.dto.OrgaoDto
import kotlinx.serialization.Serializable

@Serializable
data class OrgaosResponse(
    val dados: List<OrgaoDto>,
    val links: List<LinkDto>
)