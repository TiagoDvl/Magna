package com.tick.magna.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.analytics.AnalyticsEvent
import com.tick.magna.data.analytics.AnalyticsInterface
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.deputados.DeputadosRepositoryInterface
import com.tick.magna.data.usecases.SyncUserInformationState
import com.tick.magna.data.usecases.SyncUserInformationUseCase
import kotlin.time.TimeSource
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val dispatcher: DispatcherInterface,
    private val syncUserInformation: SyncUserInformationUseCase,
    private val deputadosRepository: DeputadosRepositoryInterface,
    private val logger: AppLoggerInterface,
    private val analytics: AnalyticsInterface,
): ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
        private const val SEARCH_TRACKING_DEBOUNCE_MS = 1_000L
    }

    private val _homeState = MutableStateFlow(HomeState())
    val homeState: StateFlow<HomeState> = _homeState.asStateFlow()

    private var syncJob: Job? = null
    private var searchJob: Job? = null

    /** Feeds the analytics debounce only. The search itself still runs on every keystroke. */
    private val searchQuery = MutableStateFlow("")

    init {
        trySync()
        trackSearchesAfterTypingStops()
    }

    fun processAction(action: HomeAction) {
        logger.d("processAction: $action", TAG)
        when (action) {
            HomeAction.RetrySync -> trySync()
            is HomeAction.SearchDeputado -> handleSearchQuery(action.query)
        }
    }

    private fun handleSearchQuery(query: String) {
        searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch(dispatcher.io) {
            deputadosRepository.getDeputados(query).collect { results ->
                logger.d("Search '$query' → ${results.size} results", TAG)
                _homeState.update { it.copy(filteredDeputados = results) }
            }
        }
    }

    fun trySync() {
        logger.d("trySync", TAG)
        syncJob?.cancel()
        syncJob = viewModelScope.launch(dispatcher.io) {
            val startedAt = TimeSource.Monotonic.markNow()
            analytics.track(AnalyticsEvent.SyncStarted)

            syncUserInformation().collect { state ->
                logger.d("syncState → $state", TAG)

                // Done and Retry are both terminal. Reporting the duration alongside the
                // outcome is how we tell "the Camara API is slow" from "it is failing".
                when (state) {
                    SyncUserInformationState.Done -> trackSyncFinished(true, startedAt)
                    SyncUserInformationState.Retry -> trackSyncFinished(false, startedAt)
                    SyncUserInformationState.Initial,
                    SyncUserInformationState.Downloading -> Unit
                }

                _homeState.update { it.copy(syncState = state) }
            }
        }
    }

    /**
     * One event per search, not one per keystroke. Typing "tabata" would otherwise report
     * six searches, five of which nobody performed.
     */
    @OptIn(FlowPreview::class)
    private fun trackSearchesAfterTypingStops() {
        viewModelScope.launch(dispatcher.io) {
            searchQuery
                .debounce(SEARCH_TRACKING_DEBOUNCE_MS)
                .filter { query -> query.isNotBlank() }
                .distinctUntilChanged()
                .collect { query ->
                    analytics.track(
                        AnalyticsEvent.SearchPerformed(
                            queryLength = query.length,
                            resultCount = _homeState.value.filteredDeputados?.size ?: 0,
                            activeFilters = 0,
                        )
                    )
                }
        }
    }

    private fun trackSyncFinished(success: Boolean, startedAt: TimeSource.Monotonic.ValueTimeMark) {
        analytics.track(
            AnalyticsEvent.SyncFinished(
                success = success,
                durationMs = startedAt.elapsedNow().inWholeMilliseconds,
            )
        )
    }
}
