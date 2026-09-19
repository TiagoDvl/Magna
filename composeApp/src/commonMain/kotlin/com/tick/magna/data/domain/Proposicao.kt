package com.tick.magna.data.domain

/**
 * How many propositions were filed in the window, which is not how many are on the screen.
 *
 * The Home shows four. The window they come from had 11333 in September 2026, and the three
 * types the section used to filter by had 1, 21 and 62 of them. Without this number a short
 * list looks the same whether it is all there is or a thirtieth of one percent.
 */
data class ProposicoesNaJanela(val total: Int, val meses: Int)

data class Proposicao(
    val id: String,
    val type: String,
    val ementa: String,
    val dataApresentacao: String,
    /**
     * Kept only for the photograph and the party of a single deputado author. The name comes
     * from [autoria], which the API gives directly and which survives an author this term's
     * deputado table has never heard of.
     */
    val autores: List<Deputado> = emptyList(),
    val url: String? = null,
    val numero: Int? = null,
    val ano: Int? = null,
    val autoria: Autoria? = null,
    /** "Aguardando Parecer", "Pronta para Pauta". Null until the proposition has moved. */
    val situacao: String? = null,
    /** Where it sits: MESA at first, then a committee. Always present, rarely interesting. */
    val orgaoSigla: String? = null,
    /** Empty for weeks after filing; two on the median once classified. */
    val temas: List<String> = emptyList(),
)

/**
 * How a proposition is named out loud: "PL 1589/2026".
 *
 * Falls back to the sigla alone when the number did not arrive, which is what every card used
 * to show — and what made two cards of the same type identical above the ementa.
 */
val Proposicao.identificacao: String
    get() = if (numero == null || ano == null) type else "$type $numero/$ano"


val proposicoesMock = listOf(
    Proposicao(
        id = "2543145",
        type = "PEC",
        ementa = "Altera a Constituição Federal e o Ato das Disposições Constitucionais Transitórias para estabelecer regras antiprivilégios para agentes públicos.",
        dataApresentacao = "2025-04-24T08:51",
        autores = deputadosMock.subList(0, 3),
        numero = 87,
        ano = 2025,
        // The one shape the old avatar row was built for, and the one it drew worst.
        autoria = Autoria("Marcelo Freixo", TipoAutor.DEPUTADO, outros = 171),
        situacao = "Pronta para Pauta",
        orgaoSigla = "CCJC",
        temas = listOf("Administração Pública", "Direito e Justiça"),
        url = "https://www.camara.leg.br/proposicoesWeb/prop_mostrarintegra?codteor=2971630"
    ),
    Proposicao(
        id = "3211789",
        type = "PL",
        ementa = "Estabelece incentivos e prioridades para provedores regionais de internet no acesso a políticas públicas de conectividade e linhas de financiamento.",
        dataApresentacao = "2025-03-15T14:23",
        autores = deputadosMock.subList(1, 2),
        numero = 1589,
        ano = 2026,
        autoria = Autoria("Tabata Amaral", TipoAutor.DEPUTADO, outros = 0),
        situacao = "Aguardando Parecer",
        orgaoSigla = "CCTI",
        temas = listOf("Ciência, Tecnologia e Inovação", "Comunicações", "Economia"),
        url = "https://www.camara.leg.br/proposicoesWeb/prop_mostrarintegra?codteor=3066749"
    ),
    Proposicao(
        id = "4521036",
        type = "PDC",
        ementa = "Susta os efeitos do Decreto nº 11.428, de 2023, que regulamenta a participação complementar da iniciativa privada na prestação de serviços públicos de saúde.",
        dataApresentacao = "2025-02-08T10:15",
        autores = emptyList(),
        numero = 412,
        ano = 2026,
        // A third of the PLs in a measured window are signed like this, and the card used to
        // draw them as an empty row.
        autoria = Autoria("Comissão de Finanças e Tributação", TipoAutor.ORGAO, outros = 0),
        url = "https://www.camara.leg.br/proposicoesWeb/prop_mostrarintegra?codteor=2845621"
    ),
    Proposicao(
        id = "1987453",
        type = "PLP",
        ementa = "Dispõe sobre o regime de tributação unificada de distribuidoras de energia elétrica e dá outras providências relacionadas ao setor energético.",
        dataApresentacao = "2025-01-19T16:42",
        autores = deputadosMock.subList(0, 2),
        url = "https://www.camara.leg.br/proposicoesWeb/prop_mostrarintegra?codteor=2789432"
    ),
    Proposicao(
        id = "5634891",
        type = "PL",
        ementa = "Institui o Programa Nacional de Apoio à Agricultura Familiar e Sustentável, estabelecendo diretrizes para financiamento e assistência técnica.",
        dataApresentacao = "2024-12-11T09:30",
        autores = deputadosMock.subList(3, 9),
        url = "https://www.camara.leg.br/proposicoesWeb/prop_mostrarintegra?codteor=2654178"
    ),
    Proposicao(
        id = "6789124",
        type = "PEC",
        ementa = "Modifica o Sistema Tributário Nacional para implementar a progressividade do Imposto sobre Propriedade de Veículos Automotores - IPVA.",
        dataApresentacao = "2024-11-28T11:05",
        autores = deputadosMock.subList(1, 3),
        url = "https://www.camara.leg.br/proposicoesWeb/prop_mostrarintegra?codteor=2598743"
    )
)