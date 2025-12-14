package com.tick.magna.data.repository

import com.tick.magna.SiglaTipo
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.source.local.dao.SiglaTipoDaoInterface
import com.tick.magna.data.source.remote.api.ProposicoesApiInterface
import kotlinx.coroutines.CoroutineScope

class ProposicoesRepository(
    private val siglaTipoDao: SiglaTipoDaoInterface,
    private val proposicoesApiInterface: ProposicoesApiInterface,
    private val loggerInterface: AppLoggerInterface,
    private val coroutineScope: CoroutineScope,
) : ProposicoesRepositoryInterface {

    companion object {
        private const val TAG = "ProposicoesRepository"
    }

    override suspend fun syncSiglaTipos(): Boolean {
        return try {
            loggerInterface.d("Started Sync for Proposições SiglaTipos", TAG)
            val siglaTiposResponse = proposicoesApiInterface.getSiglaTipos().dados
            val localSiglaTipos = siglaTiposResponse.map {
                SiglaTipo(
                    id = it.cod.toLong(),
                    sigla = it.sigla,
                    nome = it.nome,
                    descricao = it.descricao
                )
            }
            siglaTipoDao.insertSiglaTipos(localSiglaTipos)
            true

        } catch (exception: Exception) {
            loggerInterface.e("Failed to Sync Proposições SiglaTipos", exception, TAG)
            false
        }
    }
}
