package com.tick.magna.features.comissoes.permanentes.component.domain

data class ComissaoPermanente(
    val comissaoPermanenteId: String,
    val nomeResumido: String,
    val nome: String,
    /** Null while the term has not been measured; see OrgaoAtividade. */
    val votacoes: Int? = null,
)
