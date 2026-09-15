package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Only the office is read from this block. The rest of what the endpoint nests here is
 * left undeclared so it cannot fail parsing.
 */
@Serializable
data class UltimoStatusDto(
    @SerialName("gabinete")
    val gabinete: GabineteDto? = null,
)
