package com.tick.magna.data.usecases

import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.DeputadosRepositoryInterface
import com.tick.magna.data.source.local.dao.UserDaoInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow

class GetDeputadosListUseCase(
    private val deputadosRepository: DeputadosRepositoryInterface,
    private val userDao: UserDaoInterface,
    private val logger: AppLoggerInterface,
) {
    companion object Companion {
        private const val TAG = "GetDeputadosListUseCase"
    }

    operator fun invoke(): Flow<DeputadosListState> {
        return flow {
            emit(DeputadosListState.Loading)
            val user = userDao.getUser().firstOrNull()
            if (user != null && user.legislaturaId != null) {
                val deputados = deputadosRepository.getDeputados(user.legislaturaId)
                val state = if (deputados.isSuccess) {
                    logger.d("Deputados response: ${deputados.getOrNull()?.size} ", TAG)
                    DeputadosListState.Success(deputados.getOrDefault(emptyList()))
                } else {
                    logger.d("Deputados error: ${deputados.exceptionOrNull()} ", TAG)
                    DeputadosListState.Error
                }
                emit(state)
            } else {
                emit(DeputadosListState.Error)
            }
        }
    }
}

sealed interface DeputadosListState {
    data object Loading: DeputadosListState
    data class Success(val deputados: List<Deputado>): DeputadosListState
    data object Error: DeputadosListState
}