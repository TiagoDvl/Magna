package com.tick.magna.data.domain

data class DeputadoMembro(
    val id: String,
    val nome: String,
    val siglaPartido: String?,
    val siglaUf: String?,
    val urlFoto: String?,
    val email: String?,
    val sexo: String?,
    val dataNascimento: String?,
    val ufNascimento: String?,
    val municipioNascimento: String?,
)
