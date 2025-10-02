package com.tick.magna.shared.data.model

data class Deputado(
    val id: Int,
    val nome: String,
    val partido: String,
    val uf: String,
    val fotoUrl: String,
    val email: String
)