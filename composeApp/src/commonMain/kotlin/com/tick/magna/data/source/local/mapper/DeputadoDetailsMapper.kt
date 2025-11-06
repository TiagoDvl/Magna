package com.tick.magna.data.source.local.mapper

import com.tick.magna.data.domain.DeputadoDetails
import com.tick.magna.DeputadoDetails as DeputadoDetailsEntity

fun DeputadoDetailsEntity.toDomain(): DeputadoDetails {
    return DeputadoDetails(
        gabineteBuilding = gabineteBuilding,
        gabineteRoom = gabineteRoom,
        gabineteTelephone = gabineteTelephone,
        gabineteEmail = gabineteEmail,
        urlWebsite = urlWebsite,
        socials = socials?.split(", "),
    )
}