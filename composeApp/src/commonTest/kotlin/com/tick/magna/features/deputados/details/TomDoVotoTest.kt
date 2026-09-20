package com.tick.magna.features.deputados.details

import com.tick.magna.data.domain.VotoDeputado
import kotlin.test.Test
import kotlin.test.assertEquals

class TomDoVotoTest {

    @Test
    fun `the five spellings the Camara uses land in three tones`() {
        assertEquals(TomDoVoto.SIM, tomDoVoto("Sim"))
        assertEquals(TomDoVoto.NAO, tomDoVoto("Não"))
        assertEquals(TomDoVoto.OUTRO, tomDoVoto("Abstenção"))
        assertEquals(TomDoVoto.OUTRO, tomDoVoto("Obstrução"))
        assertEquals(TomDoVoto.OUTRO, tomDoVoto("Artigo 17"))
    }

    @Test
    fun `case and a missing accent do not turn a no into something else`() {
        // Falling through to OUTRO would be a wrong answer rather than a missing one: the tag
        // would be neutral for a vote that was cast.
        assertEquals(TomDoVoto.NAO, tomDoVoto("NAO"))
        assertEquals(TomDoVoto.NAO, tomDoVoto(" não "))
        assertEquals(TomDoVoto.SIM, tomDoVoto("SIM"))
    }

    @Test
    fun `the counts under the tab use the same rule as the tags`() {
        val votos = listOf(
            voto("Sim"),
            voto("NAO"),
            voto("Abstenção"),
            voto("Não"),
        )

        val content = VotosState.Content(votos)

        assertEquals(1, content.sim)
        assertEquals(2, content.nao)
        assertEquals(1, content.outros)
    }

    private fun voto(voto: String) = VotoDeputado(
        votacaoId = voto,
        dataHoraRegistro = null,
        descricao = "",
        siglaOrgao = null,
        aprovacao = true,
        proposicaoRotulo = null,
        voto = voto,
    )
}
