package com.tick.magna.data.model

data class Deputado(
    val id: Int,
    val nome: String,
    val partido: String,
    val uf: String,
    val fotoUrl: String,
    val email: String
)