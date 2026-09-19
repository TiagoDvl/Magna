package com.tick.magna.data.domain

/**
 * One vote a deputado cast, with enough of the votacao around it to mean something.
 *
 * A bare "Sim" is not information. What the Camara gives to put around it is the description,
 * which for a plenary vote reads like `Aprovado o Requerimento de Urgência (Art. 155 do RICD).
 * Sim: 320; Não: 41; Total: 361.` — the tally is written into the sentence.
 */
data class VotoDeputado(
    val votacaoId: String,
    /** As the API sends it. The screen formats it. */
    val dataHoraRegistro: String?,
    val descricao: String,
    val siglaOrgao: String?,
    val aprovacao: Boolean,
    /** As the Camara words it: `Sim`, `Não`, `Abstenção`, `Obstrução`, `Artigo 17`. */
    val voto: String,
)

val votosDeputadoMock = listOf(
    VotoDeputado(
        votacaoId = "2643915-8",
        dataHoraRegistro = "2026-09-10T16:20:11",
        descricao = "Aprovado o Requerimento de Urgência (Art. 155 do RICD). Sim: 320; Não: 41; Total: 361.",
        siglaOrgao = "PLEN",
        aprovacao = true,
        voto = "Sim",
    ),
    VotoDeputado(
        votacaoId = "2618177-82",
        dataHoraRegistro = "2026-08-20T19:02:45",
        descricao = "Rejeitada a Emenda de Plenário nº 1. Sim: 105; Não: 233; Total: 338.",
        siglaOrgao = "PLEN",
        aprovacao = false,
        voto = "Não",
    ),
    VotoDeputado(
        votacaoId = "2434783-64",
        dataHoraRegistro = "2026-07-02T11:15:00",
        descricao = "Rejeitado o Requerimento de Retirada de Pauta. Resultado:  17 votos \"Sim\", 20 votos \"Não\".",
        siglaOrgao = "CCJC",
        aprovacao = false,
        voto = "Obstrução",
    ),
)
