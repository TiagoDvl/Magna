package com.tick.magna.data.source.local.mapper

import com.tick.magna.data.domain.Partido
import com.tick.magna.Partido as PartidoEntity

fun PartidoEntity.toDomain(): Partido {
    return Partido(
        id = this.id,
    )
}