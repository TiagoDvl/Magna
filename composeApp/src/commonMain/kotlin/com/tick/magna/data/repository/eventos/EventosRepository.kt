package com.tick.magna.data.repository.eventos

import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.domain.Pauta
import com.tick.magna.data.domain.Proposicao
import com.tick.magna.data.source.remote.api.EventosApiInterface
import kotlinx.coroutines.CoroutineScope

class EventosRepository(
    private val eventosApi: EventosApiInterface,
    private val coroutineScope: CoroutineScope,
) : EventosRepositoryInterface {

    override suspend fun getEventoPautas(idEvento: String): List<Pauta> {
        val pautasResponse = eventosApi.getPautas(idEvento).dados

        return pautasResponse.map {
            Pauta(
                ordem = it.ordem,
                regime = it.regime,
                relator = it.relator?.let {
                    Deputado(
                        id = it.id,
                        name = it.nome,
                        partido = it.siglaPartido,
                        uf = it.siglaUf,
                        profilePicture = it.urlFoto,
                        email = it.email
                    )
                },
                proposicao = Proposicao(
                    id = it.proposicao.id.toString(),
                    type = it.proposicao.codTipo.toString(),
                    ementa = it.proposicao.ementa,
                    dataApresentacao = it.proposicao.dataApresentacao,
                    autores = emptyList(),
                    url = ""

                ),
                textoParecer = it.textoParecer.orEmpty(),
                situacaoItem = it.situacaoItem
            )
        }
    }
}