package com.tick.magna.data.repository.deputados

import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.domain.DeputadoDetails
import com.tick.magna.data.domain.DeputadoExpense
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.Resource
import com.tick.magna.data.repository.cachedList
import com.tick.magna.data.repository.cachedRecord
import com.tick.magna.data.source.local.dao.DeputadoDaoInterface
import com.tick.magna.data.source.local.dao.DeputadoDetailsDaoInterface
import com.tick.magna.data.source.local.dao.DeputadoExpenseDaoInterface
import com.tick.magna.data.source.local.dao.UserDaoInterface
import com.tick.magna.data.source.local.mapper.toDomain
import com.tick.magna.data.source.local.mapper.toLocal
import com.tick.magna.data.source.remote.api.DeputadosApiInterface
import com.tick.magna.data.source.remote.response.hasNextPage
import com.tick.magna.data.source.remote.dto.toLocal
import com.tick.magna.util.currentYear
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import com.tick.magna.Deputado as DeputadoEntity

/**
 * Every flow here builds its own network work inside itself, so leaving the screen
 * cancels it. Nothing is launched into a scope that outlives the caller.
 */
@ExperimentalCoroutinesApi
internal class DeputadosRepository(
    private val userDao: UserDaoInterface,
    private val deputadosApi: DeputadosApiInterface,
    private val deputadoDao: DeputadoDaoInterface,
    private val deputadoDetailsDao: DeputadoDetailsDaoInterface,
    private val deputadoExpenseDao: DeputadoExpenseDaoInterface,
    private val loggerInterface: AppLoggerInterface,
) : DeputadosRepositoryInterface {

    override fun getRecentDeputados(): Flow<List<Deputado>> {
        return deputadoDao.getRecentDeputados().map { recentDeputados ->
            recentDeputados.mapNotNull { it.toDomain() }
        }
    }

    override fun getDeputados(): Flow<List<Deputado>> = channelFlow {
        val legislaturaId = legislaturaId()
            ?: run {
                loggerInterface.w("getDeputados: no legislaturaId, returning empty", TAG)
                send(emptyList())
                return@channelFlow
            }

        launch {
            try {
                refreshDeputados(legislaturaId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (e: Exception) {
                loggerInterface.e("getDeputados: refresh failed, serving cache", e, TAG)
            }
        }

        deputadoDao.getDeputados(legislaturaId)
            .map { deputados -> deputados.mapNotNull { it.toDomain() } }
            .collect { deputados -> send(deputados) }
    }

    override fun getDeputados(query: String): Flow<List<Deputado>> {
        loggerInterface.d("getDeputados: query='$query'", TAG)

        return userDao.getUser().flatMapLatest { user ->
            val legislaturaId = user?.legislaturaId ?: return@flatMapLatest flowOf(emptyList())

            deputadoDao.getDeputados(legislaturaId, query).map { deputados ->
                deputados.mapNotNull { it.toDomain() }
            }
        }
    }

    override suspend fun syncDeputados(): Boolean {
        val legislaturaId = legislaturaId()
            ?: run {
                loggerInterface.w("syncDeputados: no legislaturaId, skipping", TAG)
                return false
            }

        return try {
            refreshDeputados(legislaturaId)
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            loggerInterface.e("syncDeputados: failed", e, TAG)
            false
        }
    }

    override fun getDeputado(deputadoId: String): Flow<Deputado> {
        return userDao.getUser().flatMapLatest { user ->
            val legislaturaId = user?.legislaturaId ?: return@flatMapLatest emptyFlow()

            deputadoDao.getDeputado(legislaturaId, deputadoId).mapNotNull { it.toDomain() }
        }
    }

    override fun getDeputadoDetails(deputadoId: String): Flow<Resource<DeputadoDetails>> {
        return userDao.getUser().flatMapLatest { user ->
            val legislaturaId = user?.legislaturaId
                ?: return@flatMapLatest flowOf(Resource.Error())

            cachedRecord(
                cache = deputadoDetailsDao.getDeputadoDetails(legislaturaId, deputadoId)
                    .map { entity -> entity?.toDomain() },
                refresh = {
                    deputadoDao.updateLastSeen(deputadoId)
                    val response = deputadosApi.getDeputadoById(deputadoId)
                    deputadoDetailsDao.insertDeputadosDetails(listOf(response.dados.toLocal(legislaturaId)))
                },
            ).logFailures("getDeputadoDetails(deputadoId=$deputadoId)")
        }
    }

    override fun getDeputadoExpenses(deputadoId: String): Flow<Resource<List<DeputadoExpense>>> {
        return userDao.getUser().flatMapLatest { user ->
            val legislaturaId = user?.legislaturaId
                ?: return@flatMapLatest flowOf(Resource.Error())

            cachedList(
                cache = deputadoExpenseDao.getDeputadoExpense(deputadoId, legislaturaId)
                    .map { expenses -> expenses.map { it.toDomain() } },
                refresh = {
                    val year = currentYear().toString()
                    val response = deputadosApi.getDeputadoExpenses(deputadoId, legislaturaId, year)
                    deputadoExpenseDao.insertDeputadoExpenses(
                        response.dados.map { it.toLocal(deputadoId, legislaturaId) }
                    )
                },
            ).logFailures("getDeputadoExpenses(deputadoId=$deputadoId)")
        }
    }

    private suspend fun legislaturaId(): String? = userDao.getUser().first()?.legislaturaId

    /**
     * Follows the `next` link instead of taking the first page and calling it the list.
     *
     * Until the legislature became selectable this was one request, and it worked by luck:
     * the current term has 879 members and the page the API hands out holds a thousand. The
     * 55th has 1138, so the old version would have stored a thousand of them and dropped the
     * rest without an error anywhere.
     *
     * Everything is collected before anything is written, so a term is stored whole or not at
     * all. A partially stored legislature looks exactly like the bug this replaces.
     */
    private suspend fun refreshDeputados(legislaturaId: String) {
        val deputados = mutableListOf<DeputadoEntity>()
        var page = FIRST_PAGE

        while (true) {
            val response = deputadosApi.getDeputados(legislaturaId = legislaturaId, page = page)
            deputados += response.dados.map { it.toLocal(legislaturaId) }

            if (!response.links.hasNextPage()) break

            if (page >= MAX_PAGES) {
                loggerInterface.w(
                    "refreshDeputados: stopped at page $page with a next link still present",
                    TAG,
                )
                break
            }
            page++
        }

        deputadoDao.insertDeputados(deputados)
        loggerInterface.i("refreshDeputados: saved ${deputados.size} deputados in $page page(s)", TAG)
    }

    private fun <T> Flow<Resource<T>>.logFailures(what: String): Flow<Resource<T>> = onEach { resource ->
        if (resource is Resource.Error) {
            loggerInterface.e("$what failed", resource.cause, TAG)
        }
    }

    private companion object {
        const val TAG = "DeputadosRepository"
        const val FIRST_PAGE = 1

        /**
         * A bound, not an expectation. The largest legislature takes two pages; this only
         * exists so a `next` link that never goes away cannot loop forever.
         */
        const val MAX_PAGES = 10
    }
}
