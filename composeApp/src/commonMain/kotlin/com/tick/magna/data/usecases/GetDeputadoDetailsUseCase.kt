package com.tick.magna.data.usecases

import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.DeputadosRepositoryInterface
import com.tick.magna.data.source.local.dao.UserDaoInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onEach

class GetDeputadoDetailsUseCase(
    private val userDao: UserDaoInterface,
    private val deputadosRepository: DeputadosRepositoryInterface,
    private val logger: AppLoggerInterface,
) {
    companion object Companion {
        private const val TAG = "GetDeputadoDetailsUseCase"
    }

    suspend operator fun invoke(deputadoId: String): Flow<Deputado?> {
        val user = userDao.getUser().firstOrNull()

        logger.d("Fetching deputado [$deputadoId] for: $user", TAG)

        return deputadosRepository.getDeputadoById(user?.legislaturaId!!, deputadoId)
            .onEach { deputado ->
                logger.d("Deputado: $deputado", TAG)
            }
    }
}
