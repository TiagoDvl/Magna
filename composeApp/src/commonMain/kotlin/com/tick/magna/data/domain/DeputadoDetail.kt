package com.tick.magna.data.domain

data class DeputadoDetails(
    val gabineteBuilding: String?,
    val gabineteRoom: String?,
    val gabineteTelephone: String?,
    val gabineteEmail: String?,
    val urlWebsite: String?,
    val socials: Map<String, String> = emptyMap(),
)

val deputadoDetailMock = DeputadoDetails(
    gabineteBuilding = "4",
    gabineteRoom = "101",
    gabineteTelephone = "2367004",
    gabineteEmail = "deputado@gmail.com",
    urlWebsite = "http://www.deputado.com.br",
    socials = mapOf(
        "Facebook" to "https://www.facebook.com/deputado",
        "Twitter" to "https://www.twitter.com/deputado",
        "Instagram" to "https://www.instagram.com/deputado",
    )
)