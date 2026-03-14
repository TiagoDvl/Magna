package com.tick.magna.data.domain

data class PartidoDetail(
    val id: Int,
    val sigla: String,
    val nome: String,
    val urlLogo: String?,
    val urlWebSite: String?,
    val urlFacebook: String?,
    val totalMembros: Int?,
    val situacao: String?,
    val lider: Lider?,
)
