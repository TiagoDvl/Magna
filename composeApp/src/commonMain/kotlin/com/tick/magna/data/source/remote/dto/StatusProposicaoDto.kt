package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.Serializable

/**
 * Where the proposition stands, as the register describes it.
 *
 * Four of these fields were arriving in every detail response and being dropped. Measured over
 * four PLs of 2023: all four carry a `regime` and an `apreciacao`, all four name a
 * `uriUltimoRelator`, and all four have a `descricaoTramitacao` that is a different fact from
 * `descricaoSituacao` — the stage the paper is at, not the state it is in.
 */
@Serializable
data class StatusProposicaoDto(
    val idSituacao: Int? = null,
    val descricaoSituacao: String? = null,
    val despacho: String? = null,
    val siglaOrgao: String? = null,
    /** When the status itself was set, which is not when the proposition was filed. */
    val dataHora: String? = null,
    /** `Urgência (Art. 155, RICD)` or `Ordinário (Art. 151, III, RICD)`. A dot when unset. */
    val regime: String? = null,
    /** Whether the plenary decides it or the committees do, which is the difference that matters. */
    val apreciacao: String? = null,
    val descricaoTramitacao: String? = null,
    /** The last rapporteur, as a URI. The id at the end of it is in the local roster. */
    val uriUltimoRelator: String? = null,
)
