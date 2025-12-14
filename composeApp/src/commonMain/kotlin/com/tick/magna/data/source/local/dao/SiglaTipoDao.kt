package com.tick.magna.data.source.local.dao

import com.tick.magna.SiglaTipo
import com.tick.magna.SiglaTipoQueries
import com.tick.magna.data.dispatcher.DispatcherInterface

class SiglaTipoDao(
    private val siglaPartidoQueries: SiglaTipoQueries,
    private val dispatcherInterface: DispatcherInterface
) : SiglaTipoDaoInterface {

    override fun insertSiglaTipos(siglaTipos: List<SiglaTipo>) {
        siglaPartidoQueries.transaction {
            siglaTipos.forEach {
                siglaPartidoQueries.insertSiglaTipo(it)
            }
        }
    }

    override fun getSiglaTipoById(siglaTipoId: String): SiglaTipo {
        return siglaPartidoQueries.getSiglaTipoById(siglaTipoId.toLong()).executeAsOne()
    }
}