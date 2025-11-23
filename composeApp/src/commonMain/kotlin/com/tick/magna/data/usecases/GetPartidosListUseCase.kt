package com.tick.magna.data.usecases

import com.tick.magna.data.domain.Partido
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.PartidosRepositoryInterface
import com.tick.magna.data.source.local.dao.UserDaoInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf

class GetPartidosListUseCase(
    private val partidoRepository: PartidosRepositoryInterface,
    private val userDao: UserDaoInterface,
    private val logger: AppLoggerInterface,
) {
    companion object Companion {
        private const val TAG = "GetPartidosListUseCase"
    }

    suspend operator fun invoke(): Flow<List<Partido>> {
        val legislaturaId = userDao.getUser().firstOrNull()?.legislaturaId ?: return flowOf(emptyList())

        return partidoRepository.getPartidos(legislaturaId)
    }
}
