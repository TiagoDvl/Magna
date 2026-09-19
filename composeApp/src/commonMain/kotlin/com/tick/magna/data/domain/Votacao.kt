package com.tick.magna.data.domain

data class Votacao(
    val id: String,
    val dataHoraRegistro: String?,
    /**
     * What the Camara records as the outcome, and it is procedural boilerplate: across every
     * committee measured it says either "Aprovado o Parecer." or "Aprovada a Redação Final.".
     * The substance is in [parecer] and [proposicoes].
     */
    val descricao: String,
    val aprovacao: Boolean,
    val proposicoes: List<ProposicaoAfetada>,
    /**
     * What the rapporteur argued, free text, straight from the API — for example "Parecer da
     * Relatora, Dep. Silvia Cristina (PP-RO), pela aprovação."
     *
     * Already downloaded with every vote detail and thrown away until now. The name and party
     * are embedded in the sentence rather than in fields of their own, so this is shown as
     * written; turning it into a link to the deputado screen would mean matching the text
     * against the stored roster, and that is a separate problem.
     */
    val parecer: String?,
    val idEvento: String?,
)

/**
 * A proposition a vote acted on.
 *
 * [rotulo] is the short name people recognise — "PL 4770/2023" — built from fields the detail
 * already returns and that were being dropped, leaving the screen showing a bare ementa with
 * nothing to call it.
 */
data class ProposicaoAfetada(
    val id: String,
    val rotulo: String?,
    val ementa: String,
)

val votacoesMock = listOf(
    Votacao(
        id = "2358471-1",
        dataHoraRegistro = "15/03/2024",
        descricao = "Aprovado o Parecer.",
        aprovacao = true,
        proposicoes = listOf(
            ProposicaoAfetada(
                id = "2256735",
                rotulo = "PL 2630/2020",
                ementa = "Institui a Lei Brasileira de Liberdade, Responsabilidade e Transparência na Internet.",
            ),
        ),
        parecer = "Parecer do Relator, Dep. Orlando Silva (PCdoB-SP), pela constitucionalidade, juridicidade e técnica legislativa.",
        idEvento = "65432",
    ),
    Votacao(
        id = "2358472-2",
        dataHoraRegistro = "15/03/2024",
        descricao = "Aprovada a Redação Final.",
        aprovacao = false,
        proposicoes = listOf(
            ProposicaoAfetada(
                id = "2196833",
                rotulo = "PEC 45/2019",
                ementa = "Altera o Sistema Tributário Nacional e dá outras providências.",
            ),
        ),
        parecer = "Parecer da Relatora, Dep. Marussa Boldrin (REPUBLIC-GO), pela aprovação, com emenda.",
        idEvento = "65433",
    ),
    Votacao(
        id = "2358473-3",
        dataHoraRegistro = "20/03/2024",
        descricao = "Aprovado o Parecer.",
        aprovacao = true,
        proposicoes = listOf(
            ProposicaoAfetada(
                id = "2412345",
                rotulo = null,
                ementa = "Dispõe sobre medidas de auxílio emergencial para regiões atingidas por desastres.",
            ),
        ),
        parecer = null,
        idEvento = "65445",
    ),
)
