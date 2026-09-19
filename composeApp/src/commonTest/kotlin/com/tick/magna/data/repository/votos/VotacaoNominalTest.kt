package com.tick.magna.data.repository.votos

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Telling the 2% that have individual votes from the 98% that do not, using the text the
 * listing already returns.
 *
 * The Camara registered 7360 votacoes in 2026 and 152 of them are nominal. Asking
 * `/votacoes/{id}/votos` to find out would be 7360 requests for 152 answers, so the
 * description is read instead — the tally is written into the sentence.
 */
class VotacaoNominalTest {

    @Test
    fun a_plenary_tally_in_the_description_is_the_signal() {
        assertTrue(
            isVotacaoNominal(
                "Aprovado o Requerimento de Urgência (Art. 155 do RICD). Sim: 320; Não: 41; Total: 361."
            )
        )
    }

    @Test
    fun a_committee_words_it_differently_and_still_counts() {
        // Dropping "Resultado:" takes the filter from 147 of 152 down to 122, because this is
        // how committees write the same thing.
        assertTrue(
            isVotacaoNominal("Rejeitado o Requerimento de Retirada de Pauta. Resultado:  17 votos \"Sim\", 20 votos \"Não\".")
        )
    }

    @Test
    fun a_symbolic_vote_has_no_tally_to_find() {
        // What the other 98% look like. These carry no individual record at all, so asking
        // for their votes returns nothing and costs a request.
        assertFalse(isVotacaoNominal("Aprovado o Parecer."))
        assertFalse(isVotacaoNominal("Aprovada a Redação Final assinada pelo relator, Dep. Isnaldo Bulhões Jr. (MDB/AL)."))
        assertFalse(isVotacaoNominal("Aprovado o Projeto de Lei, com emendas."))
    }

    @Test
    fun a_votacao_with_no_description_is_not_a_candidate() {
        assertFalse(isVotacaoNominal(null))
        assertFalse(isVotacaoNominal(""))
    }

    @Test
    fun the_word_sim_on_its_own_is_not_enough() {
        // The marker is "Sim:" with the colon, which is the tally. A description that merely
        // contains the word would make most of the 7208 symbolic votacoes candidates.
        assertFalse(isVotacaoNominal("Aprovada a emenda. Simples maioria."))
    }
}
