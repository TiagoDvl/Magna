package com.tick.magna.data.source.local.mapper

import com.tick.magna.Deputado
import com.tick.magna.Partido
import com.tick.magna.data.domain.Deputado as DeputadoDomain

fun Deputado.toDomain(partido: Partido? = null): DeputadoDomain {
    return DeputadoDomain(
        id = id,
        name = name,
        profilePicture = profile_picture,
        partido = partido?.nome,
        uf = uf,
        email = email
    )
}