package com.tick.magna.data.domain

data class Pauta(
    val ordem: String,
    val regime: String,
    val relator: Deputado?,
    val proposicao: Proposicao,
    val textoParecer: String,
    val situacaoItem: String,
)
