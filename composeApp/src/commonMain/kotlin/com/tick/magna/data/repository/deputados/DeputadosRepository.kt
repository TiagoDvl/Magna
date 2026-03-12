package com.tick.magna.data.repository.deputados

import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.domain.DeputadoExpense
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.deputados.result.DeputadoDetailsResult
import com.tick.magna.data.source.local.dao.DeputadoDaoInterface
import com.tick.magna.data.source.local.dao.DeputadoDetailsDaoInterface
import com.tick.magna.data.source.local.dao.DeputadoExpenseDaoInterface
import com.tick.magna.data.source.local.dao.UserDaoInterface
import com.tick.magna.data.source.local.mapper.toDomain
import com.tick.magna.data.source.local.mapper.toLocal
import com.tick.magna.data.source.remote.api.DeputadosApiInterface
import com.tick.magna.data.source.remote.dto.toLocal
import com.tick.magna.util.currentYear
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
internal class DeputadosRepository(
    private val userDao: UserDaoInterface,
    private val deputadosApi: DeputadosApiInterface,
    private val deputadoDao: DeputadoDaoInterface,
    private val deputadoDetailsDao: DeputadoDetailsDaoInterface,
    private val deputadoExpenseDao: DeputadoExpenseDaoInterface,
    private val loggerInterface: AppLoggerInterface,
    private val coroutineScope: CoroutineScope
) : DeputadosRepositoryInterface {

    companion object Companion {
        private const val TAG = "DeputadosRepository"
    }

    override suspend fun getRecentDeputados(): Flow<List<Deputado>> {
        loggerInterface.d("getRecentDeputados", TAG)

        return deputadoDao.getRecentDeputados().map { recentDeputados ->
            recentDeputados.mapNotNull { it.toDomain() }
        }
    }

    override suspend fun getDeputados(): Flow<List<Deputado>> {
        val legislaturaId = userDao.getUser().first()?.legislaturaId
            ?: run {
                loggerInterface.w("getDeputados: no legislaturaId, returning empty", TAG)
                return flowOf(emptyList())
            }

        return deputadoDao.getDeputados(legislaturaId).map { deputados ->
            deputados.mapNotNull { it.toDomain() }
        }.also {
            coroutineScope.launch {
                try {
                    val response = deputadosApi.getDeputados(legislaturaId = legislaturaId)
                    deputadoDao.insertDeputados(response.dados.map { it.toLocal(legislaturaId) })
                    loggerInterface.d("getDeputados: saved ${response.dados.size} deputados", TAG)
                } catch (e: Exception) {
                    loggerInterface.e("getDeputados: API call failed", e, TAG)
                }
            }
        }
    }

    override suspend fun getDeputados(query: String): Flow<List<Deputado>> {
        val legislaturaId = userDao.getUser().first()?.legislaturaId
            ?: run {
                loggerInterface.w("getDeputados(query='$query'): no legislaturaId, returning empty", TAG)
                return flowOf(emptyList())
            }

        loggerInterface.d("getDeputados: query='$query'", TAG)
        return deputadoDao.getDeputados(legislaturaId, query).map { deputados ->
            deputados.mapNotNull { it.toDomain() }
        }
    }

    override suspend fun syncDeputados(): Boolean {
        val legislaturaId = userDao.getUser().first()?.legislaturaId
            ?: run {
                loggerInterface.w("syncDeputados: no legislaturaId, skipping", TAG)
                return false
            }

        return try {
            val response = deputadosApi.getDeputados(legislaturaId = legislaturaId)
            deputadoDao.insertDeputados(response.dados.map { it.toLocal(legislaturaId) })
            loggerInterface.i("syncDeputados: synced ${response.dados.size} deputados", TAG)
            true
        } catch (e: Exception) {
            loggerInterface.e("syncDeputados: failed", e, TAG)
            false
        }
    }

    override suspend fun getDeputado(deputadoId: String): Flow<Deputado> {
        val legislaturaId = userDao.getUser().first()?.legislaturaId
            ?: run {
                loggerInterface.w("getDeputado($deputadoId): no legislaturaId, returning empty", TAG)
                return flowOf()
            }

        loggerInterface.d("getDeputado: deputadoId=$deputadoId", TAG)
        return deputadoDao.getDeputado(legislaturaId, deputadoId).mapNotNull {
            it.toDomain()
        }
    }

    override suspend fun getDeputadoDetails(deputadoId: String): Flow<DeputadoDetailsResult> {
        val legislaturaId = userDao.getUser().first()?.legislaturaId
            ?: run {
                loggerInterface.w("getDeputadoDetails($deputadoId): no legislaturaId, returning empty", TAG)
                return flowOf()
            }

        loggerInterface.d("getDeputadoDetails: deputadoId=$deputadoId", TAG)
        deputadoDao.updateLastSeen(deputadoId)

        val apiFailed = MutableStateFlow(false)

        coroutineScope.launch {
            try {
                val response = deputadosApi.getDeputadoById(deputadoId)
                deputadoDetailsDao.insertDeputadosDetails(listOf(response.dados.toLocal(legislaturaId)))
                loggerInterface.d("getDeputadoDetails: details saved for deputadoId=$deputadoId", TAG)
            } catch (e: Exception) {
                loggerInterface.e("getDeputadoDetails: API call failed for deputadoId=$deputadoId", e, TAG)
                apiFailed.value = true
            }
        }

        return combine(
            deputadoDetailsDao.getDeputadoDetails(legislaturaId, deputadoId),
            apiFailed
        ) { entity, failed ->
            when {
                entity != null -> DeputadoDetailsResult.Success(entity.toDomain())
                failed -> DeputadoDetailsResult.Error
                else -> DeputadoDetailsResult.Fetching
            }
        }
    }

    override fun getDeputadoExpenses(deputadoId: String): Flow<List<DeputadoExpense>> {
        loggerInterface.d("getDeputadoExpenses: deputadoId=$deputadoId", TAG)

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
                    ?: run {
                        loggerInterface.w("getDeputadoExpenses: no legislaturaId, skipping API call", TAG)
                        return@launch
                    }

                try {
                    val currentYear = currentYear()
                    val response = deputadosApi.getDeputadoExpenses(deputadoId, legislaturaId, currentYear.toString())
                    val expenses = response.dados.map { it.toLocal(deputadoId, legislaturaId) }
                    deputadoExpenseDao.insertDeputadoExpenses(expenses)
                    loggerInterface.d("getDeputadoExpenses: saved ${expenses.size} expenses for year=$currentYear", TAG)
                } catch (e: Exception) {
                    loggerInterface.e("getDeputadoExpenses: API call failed for deputadoId=$deputadoId", e, TAG)
                }
            }
        }
    }
}