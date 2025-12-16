package com.tick.magna.data.domain

data class Proposicao(
    val id: String,
    val type: String,
    val ementa: String,
    val dataApresentacao: String,
    val autores: List<Deputado> = emptyList(),
    val url: String? = null
)


val proposicoesMock = listOf(
    Proposicao(
        id = "2543145",
        type = "PEC",
        ementa = "Altera a Constituição Federal e o Ato das Disposições Constitucionais Transitórias para estabelecer regras antiprivilégios para agentes públicos.",
        dataApresentacao = "2025-04-24T08:51",
        autores = deputadosMock.subList(0, 3),
        url = "https://www.camara.leg.br/proposicoesWeb/prop_mostrarintegra?codteor=2971630"
    ),
    Proposicao(
        id = "3211789",
        type = "PL",
        ementa = "Estabelece incentivos e prioridades para provedores regionais de internet no acesso a políticas públicas de conectividade e linhas de financiamento.",
        dataApresentacao = "2025-03-15T14:23",
        autores = deputadosMock.subList(1, 4),
        url = "https://www.camara.leg.br/proposicoesWeb/prop_mostrarintegra?codteor=3066749"
    ),
    Proposicao(
        id = "4521036",
        type = "PDC",
        ementa = "Susta os efeitos do Decreto nº 11.428, de 2023, que regulamenta a participação complementar da iniciativa privada na prestação de serviços públicos de saúde.",
        dataApresentacao = "2025-02-08T10:15",
        autores = deputadosMock.subList(2, 5),
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
        autores = deputadosMock.subList(3, 6),
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