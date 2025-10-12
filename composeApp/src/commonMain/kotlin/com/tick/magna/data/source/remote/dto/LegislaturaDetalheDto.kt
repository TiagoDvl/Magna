package com.tick.magna.data.source.remote.dto

import com.tick.magna.data.domain.Legislatura
import kotlinx.serialization.Serializable

@Serializable
data class LegislaturaDetalheDto(
    val id: Int,
    val uri: String,
    val dataInicio: String,
    val dataFim: String
)

fun LegislaturaDetalheDto.toDomain(): Legislatura {
    return Legislatura(
        id = id.toString(),
        startDate = dataInicio,
        endDate = dataFim
    )
}