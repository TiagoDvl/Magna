package com.tick.magna.data.model

data class Deputado(
    val id: Int,
    val nome: String,
    val partido: String,
    val uf: String,
    val fotoUrl: String,
    val email: String
)

val deputadosMock = listOf(
    Deputado(
        id = 178957,
        nome = "Marcelo Freixo",
        partido = "PSOL",
        uf = "RJ",
        fotoUrl = "https://www.camara.leg.br/internet/deputado/bandep/178957.jpg",
        email = "dep.marcelofreixo@camara.leg.br"
    ),
    Deputado(
        id = 220599,
        nome = "Tabata Amaral",
        partido = "PSB",
        uf = "SP",
        fotoUrl = "https://www.camara.leg.br/internet/deputado/bandep/220599.jpg",
        email = "dep.tabataamaral@camara.leg.br"
    ),
    Deputado(
        id = 178948,
        nome = "Gleisi Hoffmann",
        partido = "PT",
        uf = "PR",
        fotoUrl = "https://www.camara.leg.br/internet/deputado/bandep/178948.jpg",
        email = "dep.gleisihoffmann@camara.leg.br"
    ),
    Deputado(
        id = 204537,
        nome = "Joice Hasselmann",
        partido = "UNIÃO",
        uf = "SP",
        fotoUrl = "https://www.camara.leg.br/internet/deputado/bandep/204537.jpg",
        email = "dep.joicehasselmann@camara.leg.br"
    ),
    Deputado(
        id = 204528,
        nome = "Baleia Rossi",
        partido = "MDB",
        uf = "SP",
        fotoUrl = "https://www.camara.leg.br/internet/deputado/bandep/204528.jpg",
        email = "dep.baleiarossi@camara.leg.br"
    ),
    Deputado(
        id = 204551,
        nome = "Erika Kokay",
        partido = "PT",
        uf = "DF",
        fotoUrl = "https://www.camara.leg.br/internet/deputado/bandep/204551.jpg",
        email = "dep.erikakokay@camara.leg.br"
    ),
    Deputado(
        id = 204394,
        nome = "Marcelo Ramos",
        partido = "PSD",
        uf = "AM",
        fotoUrl = "https://www.camara.leg.br/internet/deputado/bandep/204394.jpg",
        email = "dep.marceloramos@camara.leg.br"
    ),
    Deputado(
        id = 220600,
        nome = "Adriana Ventura",
        partido = "NOVO",
        uf = "SP",
        fotoUrl = "https://www.camara.leg.br/internet/deputado/bandep/220600.jpg",
        email = "dep.adrianaventura@camara.leg.br"
    ),
    Deputado(
        id = 204536,
        nome = "Felipe Rigoni",
        partido = "UNIÃO",
        uf = "ES",
        fotoUrl = "https://www.camara.leg.br/internet/deputado/bandep/204536.jpg",
        email = "dep.feliperigoni@camara.leg.br"
    ),
    Deputado(
        id = 204530,
        nome = "Paulo Teixeira",
        partido = "PT",
        uf = "SP",
        fotoUrl = "https://www.camara.leg.br/internet/deputado/bandep/204530.jpg",
        email = "dep.pauloteixeira@camara.leg.br"
    ),
    Deputado(
        id = 204379,
        nome = "Alessandro Molon",
        partido = "PSB",
        uf = "RJ",
        fotoUrl = "https://www.camara.leg.br/internet/deportado/bandep/204379.jpg",
        email = "dep.alessandromolon@camara.leg.br"
    ),
    Deputado(
        id = 220608,
        nome = "Sâmia Bomfim",
        partido = "PSOL",
        uf = "SP",
        fotoUrl = "https://www.camara.leg.br/internet/deputado/bandep/220608.jpg",
        email = "dep.samiabomfim@camara.leg.br"
    ),
    Deputado(
        id = 220586,
        nome = "Fernanda Melchionna",
        partido = "PSOL",
        uf = "RS",
        fotoUrl = "https://www.camara.leg.br/internet/deputado/bandep/220586.jpg",
        email = "dep.fernandamelchionna@camara.leg.br"
    ),
    Deputado(
        id = 204558,
        nome = "Luiza Erundina",
        partido = "PSOL",
        uf = "SP",
        fotoUrl = "https://www.camara.leg.br/internet/deputado/bandep/204558.jpg",
        email = "dep.luizaerundina@camara.leg.br"
    ),
    Deputado(
        id = 204442,
        nome = "Marcel van Hattem",
        partido = "NOVO",
        uf = "RS",
        fotoUrl = "https://www.camara.leg.br/internet/deputado/bandep/204442.jpg",
        email = "dep.marcelvanhattem@camara.leg.br"
    )
)

fun randomDeputado() = deputadosMock.random()