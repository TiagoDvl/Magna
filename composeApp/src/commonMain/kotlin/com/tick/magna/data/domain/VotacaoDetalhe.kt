package com.tick.magna.data.domain

/**
 * One nominal votacao as the app can show it without asking the network again.
 *
 * Everything here was already downloaded by the sweep that built the vote index, which is what
 * makes this screen free: the four hundred votes are in the table, and so is the proposicao.
 */
data class VotacaoDetalhe(
    val id: String,
    val dataHoraRegistro: String?,
    val descricao: String,
    val siglaOrgao: String?,
    val aprovacao: Boolean,
    val proposicao: ProposicaoVotada?,
    val votos: List<VotoRegistrado>,
) {
    // Through [tomDoVoto] rather than comparing to the literal, so the tally and the tags
    // below it can never disagree about what a vote was.
    val sim: Int get() = votos.count { tomDoVoto(it.voto) == TomDoVoto.SIM }
    val nao: Int get() = votos.count { tomDoVoto(it.voto) == TomDoVoto.NAO }
    val outros: Int get() = votos.size - sim - nao
}

/** The proposicao a votacao acted on, taken from `proposicoesAfetadas`. */
data class ProposicaoVotada(
    val id: String,
    /** `PLP 74/2026`, built from siglaTipo, numero and ano. Null when any of the three is missing. */
    val rotulo: String?,
    val ementa: String?,
)

/** One person's vote, with enough of them attached to be worth tapping. */
data class VotoRegistrado(
    val deputadoId: String,
    val nome: String?,
    val siglaPartido: String?,
    val siglaUf: String?,
    val urlFoto: String?,
    val voto: String,
)

val votacaoDetalheMock = VotacaoDetalhe(
    id = "2611313-31",
    dataHoraRegistro = "2026-09-03T17:29:39",
    descricao = "Aprovado o Requerimento de Urgência (Art. 155 do RICD). Sim: 320; Não: 41; Total: 361.",
    siglaOrgao = "PLEN",
    aprovacao = true,
    proposicao = ProposicaoVotada(
        id = "2611313",
        rotulo = "PLP 74/2026",
        ementa = "Dispõe sobre regras relativas a benefícios tributários e despesas obrigatórias no exercício de 2026.",
    ),
    votos = listOf(
        VotoRegistrado("204501", "Alencar Santana", "PT", "SP", null, "Não"),
        VotoRegistrado("204479", "Nicoletti", "PL", "RR", null, "Sim"),
        VotoRegistrado("220608", "Sâmia Bomfim", "PSOL", "SP", null, "Obstrução"),
    ),
)
