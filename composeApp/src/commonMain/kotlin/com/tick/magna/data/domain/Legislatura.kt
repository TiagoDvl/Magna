package com.tick.magna.data.domain

class Legislatura(
    val id: String,
    val startDate: String,
    val endDate: String
)

val mockedLegislaturas = listOf(
    Legislatura(
        id = "57",
        startDate = "2019-01-15",
        endDate = "2023-01-14"
    ),
    Legislatura(
        id = "56",
        startDate = "2023-01-15",
        endDate = "2027-01-14"
    ),
    Legislatura(
        id = "55",
        startDate = "2015-01-15",
        endDate = "2019-01-14"
    ),
    Legislatura(
        id = "54",
        startDate = "2027-01-15",
        endDate = "2027-01-15"
    ),
    Legislatura(
        id = "53",
        startDate = "2011-01-15",
        endDate = "2015-01-14"
    )
)