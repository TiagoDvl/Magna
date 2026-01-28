package com.tick.magna.data.domain

data class Votacao(
    val id: String,
    val dataHoraRegistro: String?,
    val descricao: String,
    val aprovacao: Boolean,
    val proposicoesAfetadas: List<String>,
    val idEvento: String?,
)

val votacoesMock = listOf(
    Votacao(
        id = "2358471-1",
        dataHoraRegistro = "2024-03-15T14:30:00",
        descricao = "PL 2630/2020 - Lei das Fake News - Votação do texto principal em Plenário",
        aprovacao = true,
        proposicoesAfetadas = listOf("PL 2630/2020", "PL 1429/2022"),
        idEvento = "65432"
    ),
    Votacao(
        id = "2358472-2",
        dataHoraRegistro = "2024-03-15T16:45:00",
        descricao = "PEC 45/2019 - Reforma Tributária - Destaque para votação em separado",
        aprovacao = false,
        proposicoesAfetadas = listOf("PEC 45/2019"),
        idEvento = "65433"
    ),
    Votacao(
        id = "2358473-3",
        dataHoraRegistro = "2024-03-20T10:15:00",
        descricao = "MPV 1234/2024 - Medidas de auxílio emergencial para regiões atingidas por desastres",
        aprovacao = true,
        proposicoesAfetadas = listOf("MPV 1234/2024"),
        idEvento = "65445"
    ),
    Votacao(
        id = "2358474-4",
        dataHoraRegistro = null,
        descricao = "PL 5555/2023 - Marco Legal da Inteligência Artificial no Brasil",
        aprovacao = true,
        proposicoesAfetadas = listOf("PL 5555/2023", "PL 21/2020"),
        idEvento = null
    ),
    Votacao(
        id = "2358475-5",
        dataHoraRegistro = "2024-04-02T15:00:00",
        descricao = "Requerimento de urgência para PL 1234/2024 - Regulamentação do trabalho por aplicativos",
        aprovacao = false,
        proposicoesAfetadas = listOf("PL 1234/2024"),
        idEvento = "65478"
    ),
    Votacao(
        id = "2358476-6",
        dataHoraRegistro = "2024-04-10T11:30:00",
        descricao = "PLP 68/2024 - Lei Complementar sobre ICMS em operações interestaduais",
        aprovacao = true,
        proposicoesAfetadas = listOf("PLP 68/2024"),
        idEvento = "65490"
    ),
    Votacao(
        id = "2358477-7",
        dataHoraRegistro = "2024-04-15T14:20:00",
        descricao = "PDC 234/2024 - Sustação de decreto presidencial sobre uso de armamento",
        aprovacao = false,
        proposicoesAfetadas = listOf("PDC 234/2024"),
        idEvento = "65501"
    ),
    Votacao(
        id = "2358478-8",
        dataHoraRegistro = "2024-05-05T09:45:00",
        descricao = "PL 2586/2022 - Programa Acredita - Crédito para microempreendedores",
        aprovacao = true,
        proposicoesAfetadas = listOf("PL 2586/2022", "PL 1735/2021"),
        idEvento = "65520"
    ),
    Votacao(
        id = "2358479-9",
        dataHoraRegistro = null,
        descricao = "Emenda Constitucional - Prorrogação da DRU até 2032",
        aprovacao = true,
        proposicoesAfetadas = listOf("PEC 23/2024"),
        idEvento = null
    ),
    Votacao(
        id = "2358480-10",
        dataHoraRegistro = "2024-05-20T16:10:00",
        descricao = "PL 4567/2023 - Política Nacional de Educação Digital nas escolas públicas",
        aprovacao = true,
        proposicoesAfetadas = listOf("PL 4567/2023", "PL 2738/2022", "PL 1234/2021"),
        idEvento = "65545"
    )
)