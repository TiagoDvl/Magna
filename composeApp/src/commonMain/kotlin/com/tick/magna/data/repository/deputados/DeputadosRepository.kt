package com.tick.magna.data.repository.deputados

import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.domain.DeputadoExpense
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.source.local.dao.DeputadoDaoInterface
import com.tick.magna.data.source.local.dao.DeputadoDetailsDaoInterface
import com.tick.magna.data.source.local.dao.DeputadoExpenseDaoInterface
import com.tick.magna.data.source.local.dao.UserDaoInterface
import com.tick.magna.data.source.local.mapper.toDomain
import com.tick.magna.data.source.local.mapper.toLocal
import com.tick.magna.data.source.remote.api.DeputadosApiInterface
import com.tick.magna.data.source.remote.dto.toLocal
import com.tick.magna.data.repository.deputados.result.DeputadoDetailsResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@ExperimentalCoroutinesApi
internal class DeputadosRepository(
    private val userDao: UserDaoInterface,
    private val deputadosApi: DeputadosApiInterface,
    private val deputadoDao: DeputadoDaoInterface,
    private val deputadoDetailsDao: DeputadoDetailsDaoInterface,
    private val deputadoExpenseDao: DeputadoExpenseDaoInterface,
    private val loggerInterface: AppLoggerInterface,
    private val coroutineScope: CoroutineScope
): DeputadosRepositoryInterface {

    companion object Companion {
        private const val TAG = "DeputadosRepository"
    }

    override suspend fun getRecentDeputados(): Flow<List<Deputado>> {
        loggerInterface.d("Fetching recent deputados", TAG)

        return deputadoDao.getRecentDeputados().map { recentDeputados ->
            recentDeputados.mapNotNull { it.toDomain() }
        }
    }

    override suspend fun getDeputados(): Flow<List<Deputado>> {
        val legislaturaId = userDao.getUser().first()?.legislaturaId ?: return flowOf(emptyList())
        loggerInterface.d("getDeputados for legislatura ID: $legislaturaId", TAG)

        return deputadoDao.getDeputados(legislaturaId).map { deputados ->
            deputados.mapNotNull { it.toDomain() }
        }.also {
            coroutineScope.launch {
                try {
                    loggerInterface.d("Fetching deputados for legislatura ID: $legislaturaId", TAG)
                    val deputadosResponse = deputadosApi.getDeputados(legislaturaId = legislaturaId)

                    deputadoDao.insertDeputados(deputadosResponse.dados.map { response -> response.toLocal(legislaturaId) })
                } catch (e: Exception) {
                    loggerInterface.d("Failed to fetch deputados: ${e.message}", TAG)
                }
            }
        }
    }

    override suspend fun getDeputados(query: String): Flow<List<Deputado>> {
        val legislaturaId = userDao.getUser().first()?.legislaturaId ?: return flowOf(emptyList())
        loggerInterface.d("getDeputados for for query: $query", TAG)

        return deputadoDao.getDeputados(legislaturaId, query).map { deputados ->
            deputados.mapNotNull { it.toDomain() }
        }
    }

    override suspend fun syncDeputados(): Boolean {
        val legislaturaId = userDao.getUser().first()?.legislaturaId ?: return false
        loggerInterface.d("SyncDeputados for legislatura ID: $legislaturaId", TAG)

        return try {
            loggerInterface.d("Fetching deputados for legislatura ID: $legislaturaId", TAG)
            val deputadosResponse = deputadosApi.getDeputados(legislaturaId = legislaturaId)

            deputadoDao.insertDeputados(deputadosResponse.dados.map { response -> response.toLocal(legislaturaId) })

            true
        } catch (e: Exception) {
            loggerInterface.d("Failed to fetch deputados: ${e.message}", TAG)
            false
        }
    }

    override suspend fun getDeputado(deputadoId: String): Flow<Deputado> {
        val legislaturaId = userDao.getUser().first()?.legislaturaId ?: return flowOf()

        return deputadoDao.getDeputado(legislaturaId, deputadoId).mapNotNull {
            it.toDomain()
        }
    }

    override suspend fun getDeputadoDetails(deputadoId: String): Flow<DeputadoDetailsResult> {
        val legislaturaId = userDao.getUser().first()?.legislaturaId ?: return flowOf()

        loggerInterface.d("getDeputadoDetails for legislatura ID: $legislaturaId", TAG)
        deputadoDao.updateLastSeen(deputadoId)

        return deputadoDetailsDao.getDeputadoDetails(legislaturaId, deputadoId).mapNotNull { deputadoDetailsEntity ->
            if (deputadoDetailsEntity == null) {
                DeputadoDetailsResult.Fetching
            } else {
                DeputadoDetailsResult.Success(deputadoDetailsEntity.toDomain())
            }
        }.also {
            coroutineScope.launch {
                try {
                    loggerInterface.d("Fetching deputado details for legislatura ID: $legislaturaId", TAG)
                    val response = deputadosApi.getDeputadoById(deputadoId)

                    deputadoDetailsDao.insertDeputadosDetails(listOf(response.dados.toLocal(legislaturaId)))
                } catch (e: Exception) {
                    loggerInterface.d("Failed to fetch deputado details: ${e.message}", TAG)
                }
            }
        }
    }

    override fun getDeputadoExpenses(deputadoId: String): Flow<List<DeputadoExpense>> {
        return userDao.getUser().flatMapLatest { user ->
            if (user != null && user.legislaturaId != null) {
                deputadoExpenseDao.getDeputadoExpense(deputadoId, user.legislaturaId)
            } else {
                flowOf(emptyList())
            }
        }.map { expenses ->
            expenses.map { it.toDomain() }
        }.also {
            coroutineScope.launch {
                val legislaturaId = userDao.getUser().first()?.legislaturaId
                val currentYear = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year

                if (legislaturaId != null) {
                    try {
                        val response = deputadosApi.getDeputadoExpenses(deputadoId, legislaturaId, currentYear.toString())
                        val expenses = response.dados.map { it.toLocal(deputadoId, legislaturaId) }
                        deputadoExpenseDao.insertDeputadoExpenses(expenses)
                    } catch (exception: Exception) {
                        loggerInterface.d("Failed to fetch deputado expense: ${exception.message}", TAG)
                    }
                }
            }
        }
    }
}