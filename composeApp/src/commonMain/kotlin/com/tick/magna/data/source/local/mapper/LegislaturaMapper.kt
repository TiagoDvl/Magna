package com.tick.magna.data.source.local.mapper

import com.tick.magna.Legislatura
import com.tick.magna.data.domain.Legislatura as LegislaturaDomain

fun Legislatura.toDomain(): LegislaturaDomain {
    return LegislaturaDomain(
        id = this.id,
        startDate = this.startDate,
        endDate = this.endDate
    )
}