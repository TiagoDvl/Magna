package com.tick.magna.data.source.local.mapper

import com.tick.magna.Deputado
import com.tick.magna.data.domain.Deputado as DeputadoDomain

fun Deputado.toDomain(): DeputadoDomain? {
    if (name == null) return null

    return DeputadoDomain(
        id = id,
        name = name,
        profilePicture = profile_picture,
        partido = partido,
        uf = uf,
        email = email,
        // Null stays null: a term synced before the column existed has not been measured, and
        // that is different from having been measured as absent.
        emExercicio = emExercicio?.let { it == 1L },
    )
}