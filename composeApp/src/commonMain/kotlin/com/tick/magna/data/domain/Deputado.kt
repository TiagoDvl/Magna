package com.tick.magna.data.domain

data class Deputado(
    val id: String,
    val name: String,
    val partido: String?,
    val uf: String,
    val profilePicture: String?,
    val email: String?
)

val deputadosMock = listOf(
    Deputado(
        id = "178957",
        name = "Marcelo Freixo",
        partido = "PSOL",
        uf = "RJ",
        profilePicture = "https://www.camara.leg.br/internet/deputado/bandep/178957.jpg",
        email = "dep.marcelofreixo@camara.leg.br"
    ),
    Deputado(
        id = "220599",
        name = "Tabata Amaral",
        partido = "PSB",
        uf = "SP",
        profilePicture = "https://www.camara.leg.br/internet/deputado/bandep/220599.jpg",
        email = "dep.tabataamaral@camara.leg.br"
    ),
    Deputado(
        id = "178948",
        name = "Gleisi Hoffmann",
        partido = "PT",
        uf = "PR",
        profilePicture = "https://www.camara.leg.br/internet/deputado/bandep/178948.jpg",
        email = "dep.gleisihoffmann@camara.leg.br"
    ),
    Deputado(
        id = "204537",
        name = "Joice Hasselmann",
        partido = "UNIÃO",
        uf = "SP",
        profilePicture = "https://www.camara.leg.br/internet/deputado/bandep/204537.jpg",
        email = "dep.joicehasselmann@camara.leg.br"
    ),
    Deputado(
        id = "204528",
        name = "Baleia Rossi",
        partido = "MDB",
        uf = "SP",
        profilePicture = "https://www.camara.leg.br/internet/deputado/bandep/204528.jpg",
        email = "dep.baleiarossi@camara.leg.br"
    ),
    Deputado(
        id = "204551",
        name = "Erika Kokay",
        partido = "PT",
        uf = "DF",
        profilePicture = "https://www.camara.leg.br/internet/deputado/bandep/204551.jpg",
        email = "dep.erikakokay@camara.leg.br"
    ),
    Deputado(
        id = "204394",
        name = "Marcelo Ramos",
        partido = "PSD",
        uf = "AM",
        profilePicture = "https://www.camara.leg.br/internet/deputado/bandep/204394.jpg",
        email = "dep.marceloramos@camara.leg.br"
    ),
    Deputado(
        id = "220600",
        name = "Adriana Ventura",
        partido = "NOVO",
        uf = "SP",
        profilePicture = "https://www.camara.leg.br/internet/deputado/bandep/220600.jpg",
        email = "dep.adrianaventura@camara.leg.br"
    ),
    Deputado(
        id = "204536",
        name = "Felipe Rigoni",
        partido = "UNIÃO",
        uf = "ES",
        profilePicture = "https://www.camara.leg.br/internet/deputado/bandep/204536.jpg",
        email = "dep.feliperigoni@camara.leg.br"
    ),
    Deputado(
        id = "204530",
        name = "Paulo Teixeira",
        partido = "PT",
        uf = "SP",
        profilePicture = "https://www.camara.leg.br/internet/deputado/bandep/204530.jpg",
        email = "dep.pauloteixeira@camara.leg.br"
    ),
    Deputado(
        id = "204379",
        name = "Alessandro Molon",
        partido = "PSB",
        uf = "RJ",
        profilePicture = "https://www.camara.leg.br/internet/deportado/bandep/204379.jpg",
        email = "dep.alessandromolon@camara.leg.br"
    ),
    Deputado(
        id = "220608",
        name = "Sâmia Bomfim",
        partido = "PSOL",
        uf = "SP",
        profilePicture = "https://www.camara.leg.br/internet/deputado/bandep/220608.jpg",
        email = "dep.samiabomfim@camara.leg.br"
    ),
    Deputado(
        id = "220586",
        name = "Fernanda Melchionna",
        partido = "PSOL",
        uf = "RS",
        profilePicture = "https://www.camara.leg.br/internet/deputado/bandep/220586.jpg",
        email = "dep.fernandamelchionna@camara.leg.br"
    ),
    Deputado(
        id = "204558",
        name = "Luiza Erundina",
        partido = "PSOL",
        uf = "SP",
        profilePicture = "https://www.camara.leg.br/internet/deputado/bandep/204558.jpg",
        email = "dep.luizaerundina@camara.leg.br"
    ),
    Deputado(
        id = "204442",
        name = "Marcel van Hattem",
        partido = "NOVO",
        uf = "RS",
        profilePicture = "https://www.camara.leg.br/internet/deputado/bandep/204442.jpg",
        email = "dep.marcelvanhattem@camara.leg.br"
    )
)

fun randomDeputado() = deputadosMock.random()