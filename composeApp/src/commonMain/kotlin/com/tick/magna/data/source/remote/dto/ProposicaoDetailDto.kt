package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProposicaoDetailDto(
    val id: String,
    val siglaTipo: String = "",
    val numero: Int = 0,
    val ano: Int = 0,
    val ementa: String = "",
    val dataApresentacao: String = "",
    val urlInteiroTeor: String? = null,
    val statusProposicao: StatusProposicaoDto? = null,
    /** `Projeto de Lei`, `Requerimento de Voto de regozijo ou louvor`. What the sigla stands for. */
    val descricaoTipo: String? = null,
    /** A longer summary when the register has one; present in two of four measured. */
    val ementaDetalhada: String? = null,
    /** One comma-separated string, not a list. Present in all four measured. */
    val keywords: String? = null,
)
