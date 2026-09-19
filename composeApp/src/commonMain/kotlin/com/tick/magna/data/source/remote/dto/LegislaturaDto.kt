package com.tick.magna.data.source.remote.dto

import com.tick.magna.data.domain.Legislatura
import kotlinx.serialization.Serializable
import com.tick.magna.Legislatura as LegislaturaEntity

/**
 * One legislature, as `/legislaturas` returns it.
 *
 * The dates are what makes this worth storing: several endpoints reject `idLegislatura`
 * and only accept `dataInicio`/`dataFim`, so this is where that window comes from instead
 * of being written by hand somewhere.
 *
 * They default to empty rather than being required, following the rest of the DTOs, and a
 * record that arrives without them is dropped when it is mapped — the table declares both
 * as NOT NULL, and a legislature without a period cannot answer the question it exists for.
 */
@Serializable
data class LegislaturaDto(
    val id: Int,
    val uri: String = "",
    val dataInicio: String = "",
    val dataFim: String = "",
)

fun LegislaturaDto.toDomain(): Legislatura {
    return Legislatura(
        id = id.toString(),
        startDate = dataInicio,
        endDate = dataFim,
    )
}

fun LegislaturaDto.toLocal(): LegislaturaEntity {
    return LegislaturaEntity(
        id = id.toString(),
        startDate = dataInicio,
        endDate = dataFim,
    )
}

fun LegislaturaDto.hasPeriod(): Boolean = dataInicio.isNotBlank() && dataFim.isNotBlank()
