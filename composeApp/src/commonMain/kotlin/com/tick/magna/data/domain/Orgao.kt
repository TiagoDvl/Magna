package com.tick.magna.data.domain

data class Orgao(
    val id: String,
    val sigla: String?,
    val nome: String?,
    val nomeResumido: String?,
)

val orgaosMock = listOf(
    Orgao(
        id = "1",
        sigla = "MS",
        nome = "Ministério da Saúde",
        nomeResumido = "Saúde"
    ),
    Orgao(
        id = "2",
        sigla = "MEC",
        nome = "Ministério da Educação",
        nomeResumido = "Educação"
    ),
    Orgao(
        id = "3",
        sigla = "MJ",
        nome = "Ministério da Justiça e Segurança Pública",
        nomeResumido = "Justiça"
    )
)