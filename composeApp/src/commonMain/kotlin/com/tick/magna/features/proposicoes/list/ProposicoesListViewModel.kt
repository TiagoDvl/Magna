package com.tick.magna.features.proposicoes.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.analytics.AnalyticsEvent
import com.tick.magna.data.analytics.AnalyticsInterface
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.domain.ProposicaoBucket
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.proposicoes.contagensPorBucket
import com.tick.magna.data.repository.proposicoes.ProposicoesRepositoryInterface
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ProposicoesListViewModel(
    private val proposicoesRepository: ProposicoesRepositoryInterface,
    private val dispatcher: DispatcherInterface,
    private val logger: AppLoggerInterface,
    private val analytics: AnalyticsInterface,
) : ViewModel() {

    companion object {
        private const val TAG = "ProposicoesListViewModel"

        /** What one request to the Camara brings back, and what the SQL limit grows by. */
        private const val PAGE_SIZE = 20
    }

    private val _state = MutableStateFlow(ProposicoesListState())
    val state: StateFlow<ProposicoesListState> = _state.asStateFlow()

    private var listaJob: Job? = null
    private var paginaJob: Job? = null

    /** 1-based, as the Camara counts, and reset whenever the filter changes. */
    private var pagina = 1

    init {
        abrir(filtro = null)

        // Every chip's number at once, so the counts are there before anything is tapped. Four
        // requests of one record each — the window total and the three buckets that have a
        // list of siglas — and Tramitacao falls out of the subtraction.
        viewModelScope.launch(dispatcher.io) {
            val total = proposicoesRepository.contarNaJanela()?.total
            val fechados = ProposicaoBucket.fechados.associateWith { bucket ->
                proposicoesRepository.contarNaJanela(bucket.siglas)?.total
            }

            val contagens = contagensPorBucket(total = total, fechados = fechados)
            logger.d("contagens: $contagens", TAG)
            _state.update { it.copy(contagens = contagens) }
        }
    }

    fun onFiltroSelected(filtro: ProposicaoBucket?) {
        if (_state.value.filtro == filtro) return

        analytics.track(AnalyticsEvent.ProposicaoFilterChanged(filtro?.name ?: "TODAS"))
        abrir(filtro)
    }

    /**
     * Asks for the next page, once.
     *
     * The guard is the whole of it: the scroll condition goes true for several frames in a
     * row, and without this the same page would be fetched as many times as it stayed true —
     * sixty requests each.
     */
    fun onCarregarMais() {
        val atual = _state.value
        if (atual.carregandoMais || !atual.temMais || atual.isLoading) return

        _state.update { it.copy(carregandoMais = true) }

        paginaJob?.cancel()
        paginaJob = viewModelScope.launch(dispatcher.io) {
            val proxima = pagina + 1

            val temMais = try {
                proposicoesRepository.carregarPagina(atual.filtro, proxima)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                logger.e("carregarPagina $proxima falhou", error, TAG)
                // The rows already on screen stay. Stopping here rather than flipping temMais
                // off lets the reader scroll again and retry.
                _state.update { it.copy(carregandoMais = false) }
                return@launch
            }

            pagina = proxima
            observar(atual.filtro, pagina)
            _state.update { it.copy(carregandoMais = false, temMais = temMais) }
        }
    }

    fun onProposicaoOpened() {
        analytics.track(AnalyticsEvent.ProposicaoOpened)
    }

    /** A filter change is a new list: page one, cache re-read, first page fetched. */
    private fun abrir(filtro: ProposicaoBucket?) {
        pagina = 1
        paginaJob?.cancel()

        _state.update {
            it.copy(
                filtro = filtro,
                proposicoes = emptyList(),
                isLoading = true,
                isError = false,
                carregandoMais = false,
                temMais = true,
            )
        }

        observar(filtro, pagina)

        paginaJob = viewModelScope.launch(dispatcher.io) {
            val temMais = try {
                proposicoesRepository.carregarPagina(filtro, pagina = 1)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                logger.e("carregarPagina 1 falhou", error, TAG)
                // Only an error if there was nothing cached to fall back on. Last week's rows
                // beat an error screen because the API happens to be down right now.
                _state.update { it.copy(isLoading = false, isError = it.proposicoes.isEmpty()) }
                return@launch
            }

            _state.update { it.copy(isLoading = false, temMais = temMais) }
        }
    }

    /**
     * One collector at a time over the cache, re-subscribed when the limit grows.
     *
     * Re-subscribing costs a SQL query and emits immediately, and the new rows are a superset
     * of the old ones, so nothing blinks. The network is not involved: that is [onCarregarMais].
     */
    private fun observar(filtro: ProposicaoBucket?, pagina: Int) {
        listaJob?.cancel()
        listaJob = viewModelScope.launch(dispatcher.io) {
            proposicoesRepository
                .observeProposicoesPaginadas(filtro, limite = pagina * PAGE_SIZE)
                .collect { proposicoes ->
                    _state.update { it.copy(proposicoes = proposicoes) }
                }
        }
    }
}
