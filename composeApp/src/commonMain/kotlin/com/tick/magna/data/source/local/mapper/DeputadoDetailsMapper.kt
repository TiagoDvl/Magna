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
        socials = buildMap {
            socials?.split(", ")?.forEach { social ->
                when {
                    social.contains("facebook", ignoreCase = true) -> put("Facebook", social)
                    social.contains("twitter", ignoreCase = true) -> put("Twitter", social)
                    social.contains("instagram", ignoreCase = true) -> put("Instagram", social)
                    social.contains("youtube", ignoreCase = true) -> put("Youtube", social)
                }
            }
        },
    )
}
