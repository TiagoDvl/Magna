package com.tick.magna.data.domain

data class DeputadoVotacao(
    val id: String,
    val dataHoraVoto: String?,
    val tipoVoto: String,
    val descricao: String,
    val uriVotacao: String?,
    val siglaOrgao: String?,
    val proposicoes: List<ProposicaoVotada>,
)

data class ProposicaoVotada(
    val siglaTipo: String,
    val numero: Int,
    val ano: Int,
    val ementa: String,
    val uri: String?,
)

val deputadoVotacoesMock = listOf(
    DeputadoVotacao(
        id = "2358471-1",
        dataHoraVoto = "2024-03-15T14:30:00",
        tipoVoto = "Sim",
        descricao = "Aprovação do texto substitutivo apresentado pelo Relator ao PL 2630/2020",
        uriVotacao = "https://dadosabertos.camara.leg.br/api/v2/votacoes/2358471-1",
        siglaOrgao = "Plenário",
        proposicoes = listOf(
            ProposicaoVotada(
                siglaTipo = "PL",
                numero = 2630,
                ano = 2020,
                ementa = "Institui a Lei Brasileira de Liberdade, Responsabilidade e Transparência na Internet",
                uri = "https://www.camara.leg.br/proposicoesWeb/fichadetramitacao?idProposicao=2256735",
            )
        )
    ),
    DeputadoVotacao(
        id = "2358400-2",
        dataHoraVoto = "2024-03-10T16:15:00",
        tipoVoto = "Não",
        descricao = "Votação do requerimento de urgência para o PL 1087/2023",
        uriVotacao = "https://dadosabertos.camara.leg.br/api/v2/votacoes/2358400-2",
        siglaOrgao = "Plenário",
        proposicoes = listOf(
            ProposicaoVotada(
                siglaTipo = "PL",
                numero = 1087,
                ano = 2023,
                ementa = "Altera a Consolidação das Leis do Trabalho (CLT)",
                uri = null,
            )
        )
    ),
    DeputadoVotacao(
        id = "2358320-3",
        dataHoraVoto = "2024-02-28T11:00:00",
        tipoVoto = "Abstenção",
        descricao = "Votação do destaque ao PL 4173/2023 - Regulamentação de apostas esportivas",
        uriVotacao = "https://dadosabertos.camara.leg.br/api/v2/votacoes/2358320-3",
        siglaOrgao = "CCJC",
        proposicoes = emptyList(),
    ),
    DeputadoVotacao(
        id = "2358210-1",
        dataHoraVoto = "2024-02-20T15:45:00",
        tipoVoto = "Sim",
        descricao = "Aprovação do PL 4617/2023 em turno único",
        uriVotacao = "https://dadosabertos.camara.leg.br/api/v2/votacoes/2358210-1",
        siglaOrgao = "Plenário",
        proposicoes = listOf(
            ProposicaoVotada(
                siglaTipo = "PL",
                numero = 4617,
                ano = 2023,
                ementa = "Dispõe sobre a reforma tributária para micro e pequenas empresas",
                uri = "https://www.camara.leg.br/proposicoesWeb/fichadetramitacao?idProposicao=2389421",
            ),
            ProposicaoVotada(
                siglaTipo = "MPV",
                numero = 1234,
                ano = 2024,
                ementa = "Medida provisória para incentivo ao setor de tecnologia",
                uri = null,
            )
        )
    ),
    DeputadoVotacao(
        id = "2357990-5",
        dataHoraVoto = "2024-01-30T10:20:00",
        tipoVoto = "Obstrução",
        descricao = "Votação do requerimento para retirada de pauta do PEC 45/2019",
        uriVotacao = null,
        siglaOrgao = "Plenário",
        proposicoes = emptyList(),
    ),
)
