package com.tick.magna.data.domain

data class ProposicaoDetail(
    val id: String,
    val siglaTipo: String,
    val numero: Int,
    val ano: Int,
    val ementa: String,
    val dataApresentacao: String,
    val urlInteiroTeor: String?,
    val descricaoSituacao: String?,
    val despacho: String?,
    val orgaoSigla: String?,
)
