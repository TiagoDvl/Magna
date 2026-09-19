package com.tick.magna.data.source.remote.response

import com.tick.magna.data.source.remote.dto.OrgaoDetalheDto
import kotlinx.serialization.Serializable

@Serializable
data class OrgaoDetalheResponse(val dados: OrgaoDetalheDto)
