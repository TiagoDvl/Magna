package com.tick.magna.data.domain

data class PartidoDetail(
    val id: Int,
    val sigla: String,
    val nome: String,
    val urlLogo: String?,
    /**
     * How many deputados took office for this party at the start of the term.
     *
     * Kept alongside [totalMembros] because the pair is the only thing in this record that
     * says a party changed: the PT of the 57th started with 68 and holds 65, and neither
     * number alone carries that.
     */
    val totalPosse: Int?,
    val totalMembros: Int?,
    val situacao: String?,
    /** When the register last restated the two totals above. */
    val dataStatus: String?,
    val lider: Lider?,
    /** Raw ARGB off the logo, or null where the register hosts no logo. */
    val cor: Int?,
)
