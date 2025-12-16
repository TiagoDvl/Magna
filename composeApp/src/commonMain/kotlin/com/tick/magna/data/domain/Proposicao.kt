package com.tick.magna.data.domain

data class Proposicao(
    val id: String,
    val type: String,
    val ementa: String,
    val dataApresentacao: String,
    val autores: List<Deputado> = emptyList(),
    val url: String? = null
)
