package com.tick.magna.data.source.remote.dto

import com.tick.magna.data.domain.Legislatura
import com.tick.magna.Legislatura as LegislaturaEntity
import kotlinx.serialization.Serializable

@Serializable
data class LegislaturaDto(
    val id: Int,
    val uri: String,
    val dataInicio: String,
    val dataFim: String
)

fun LegislaturaDto.toDomain(): Legislatura {
    return Legislatura(
        id = this.id.toString(),
        startDate = this.dataInicio,
        endDate = this.dataFim
    )
}

fun LegislaturaDto.toLocal(): LegislaturaEntity {
    return LegislaturaEntity(
        id = this.id.toString(),
        startDate = this.dataInicio,
        endDate = this.dataFim
    )
}