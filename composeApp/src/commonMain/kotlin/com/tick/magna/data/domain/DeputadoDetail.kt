package com.tick.magna.data.domain

data class DeputadoDetails(
    val gabineteBuilding: String?,
    val gabineteRoom: String?,
    val gabineteTelephone: String?,
    val gabineteEmail: String?,
    val urlWebsite: String?,
    val socials: List<String>?,
)

val deputadoDetailMock = DeputadoDetails(
    gabineteBuilding = "4",
    gabineteRoom = "101",
    gabineteTelephone = "2367004",
    gabineteEmail = "deputado@gmail.com",
    urlWebsite = "http://www.deputado.com.br",
    socials = listOf(
        "https://www.facebook.com/deputado",
        "https://www.twitter.com/deputado",
        "https://www.instagram.com/deputado",
    )
)