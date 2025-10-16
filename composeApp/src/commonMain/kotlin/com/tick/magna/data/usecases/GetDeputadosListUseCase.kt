package com.tick.magna.data.usecases

import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.repository.DeputadosRepositoryInterface
import com.tick.magna.data.source.local.dao.UserDaoInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetDeputadosListUseCase(
    private val deputadosRepository: DeputadosRepositoryInterface,
    private val userDao: UserDaoInterface,
    private val logger: AppLoggerInterface,
) {
    companion object Companion {
        private const val TAG = "GetDeputadosListUseCase"
    }

    suspend operator fun invoke(): Flow<DeputadosListState> {
        return userDao.getUser().map { user ->
            if (user.legislaturaId != null) {
                val deputados = deputadosRepository.getDeputados(user.legislaturaId)
                if (deputados.isSuccess) {
                    logger.d("Deputados response: ${deputados.getOrNull()} ", TAG)
                    DeputadosListState.Success(deputados.getOrDefault(emptyList()))
                } else {
                    logger.d("Deputados error: ${deputados.exceptionOrNull()} ", TAG)
                    DeputadosListState.Error
                }
            } else {
                DeputadosListState.NoLegislaturaConfigured
            }
        }
    }
}

sealed interface DeputadosListState {
    data object Loading: DeputadosListState
    data object NoLegislaturaConfigured: DeputadosListState
    data class Success(val deputados: List<Deputado>): DeputadosListState
    data object Error: DeputadosListState
}