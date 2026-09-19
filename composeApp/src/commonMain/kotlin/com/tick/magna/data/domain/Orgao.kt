package com.tick.magna.data.domain

data class Orgao(
    val id: String,
    val sigla: String?,
    val nome: String?,
    val nomeResumido: String?,
    /**
     * Votes counted in a sample of the selected term, which is what orders the list.
     *
     * Null until the term has been measured; the list is alphabetical until then rather than
     * in some arbitrary order pretending to be a ranking.
     */
    val votacoes: Int? = null,
)
