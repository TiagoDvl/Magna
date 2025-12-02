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
        email = email
    )
}