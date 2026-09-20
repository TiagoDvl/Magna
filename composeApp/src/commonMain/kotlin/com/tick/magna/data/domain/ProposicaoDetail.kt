package com.tick.magna.data.domain

/**
 * One proposition, as its own detail endpoint describes it.
 *
 * Most of this was arriving and being discarded. The screen showed an ementa, a situação and a
 * despacho; the same response also says what the sigla stands for, what the thing is about,
 * whether it is urgent, who decides it and who is reporting on it.
 */
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
    /** `Projeto de Lei`. What `PL` means, for the reader who does not already know. */
    val descricaoTipo: String? = null,
    /** A fuller summary. Only shown when the register has one and it is not the ementa again. */
    val ementaDetalhada: String? = null,
    /** Split out of the register's single comma-separated string. */
    val keywords: List<String> = emptyList(),
    /** `Urgência (Art. 155, RICD)`. Null when the register writes its own placeholder dot. */
    val regime: String? = null,
    /** Whether the plenary decides it or the committees do. */
    val apreciacao: String? = null,
    /** The stage, which is not the situação: `Recebimento` against `Aguardando Relator`. */
    val descricaoTramitacao: String? = null,
    /** Only when the last rapporteur sat in the selected term; otherwise their name is unknown. */
    val relator: Deputado? = null,
) {

    /**
     * Whether there is anything to put under a "Situação" heading.
     *
     * Asked before the section is drawn rather than inside it. A REQ filed this month has no
     * situação, no regime and no rapporteur, and the screen used to answer that with the word
     * "Situação" over an empty card.
     */
    val temSituacao: Boolean
        get() = descricaoSituacao != null || descricaoTramitacao != null || relator != null ||
            regime != null || apreciacao != null || despacho?.isNotBlank() == true
}

/**
 * One step of the passage, most recent first when a screen shows them.
 *
 * Measured over four PLs of 2023: 60, 71, 78 and 109 steps. The whole history is a document,
 * not a section, so a screen shows the end of it and says how long it is.
 */
data class TramitacaoProposicao(
    val sequencia: Int,
    val dataHora: String?,
    val siglaOrgao: String?,
    val descricaoTramitacao: String?,
    val despacho: String?,
)

/**
 * A votacao this proposition went through, as the listing describes it.
 *
 * Enough to draw a row and open [VotacaoDetalhe]; the roll itself costs a request the list
 * does not need.
 */
data class VotacaoDaProposicao(
    val id: String,
    val dataHoraRegistro: String?,
    val siglaOrgao: String?,
    val descricao: String?,
    val aprovacao: Boolean,
)
