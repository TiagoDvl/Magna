package com.tick.magna.data.domain

data class Votacao(
    val id: String,
    val dataHoraRegistro: String?,
    val descricao: String,
    val aprovacao: Boolean,
    val proposicoesAfetadas: List<String>,
    val idEvento: String?,
)
