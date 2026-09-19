package com.tick.magna.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.analytics.AnalyticsEvent
import com.tick.magna.data.analytics.AnalyticsInterface
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.deputados.DeputadosRepositoryInterface
import com.tick.magna.data.repository.legislaturas.LegislaturasRepositoryInterface
import com.tick.magna.data.repository.user.UserRepositoryInterface
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
    private val legislaturasRepository: LegislaturasRepositoryInterface,
    private val userRepository: UserRepositoryInterface,
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

    /**
     * Whether the sync currently running belongs to a term switch. Survives a retry on purpose
     * and is only cleared once the switch finishes, because a retry of a switch is still one.
     */
    private var switchingLegislatura = false

    /** Feeds the analytics debounce only. The search itself still runs on every keystroke. */
    private val searchQuery = MutableStateFlow("")

    init {
        trySync()
        trackSearchesAfterTypingStops()
        observeLegislatura()
    }

    fun processAction(action: HomeAction) {
        logger.d("processAction: $action", TAG)
        when (action) {
            HomeAction.RetrySync -> trySync()
            is HomeAction.SearchDeputado -> handleSearchQuery(action.query)
            HomeAction.SearchResultOpened ->
                analytics.track(AnalyticsEvent.DeputadoOpened(AnalyticsEvent.Source.HOME_SEARCH))
            is HomeAction.SelectLegislatura -> selectLegislatura(action.legislaturaId)
        }
    }

    /**
     * Both the chosen term and the list to choose from, kept as flows so the selector follows
     * the database instead of a copy of it. The list fills in on the first sync; before that
     * the selector has a single entry and nothing to switch to.
     */
    private fun observeLegislatura() {
        viewModelScope.launch(dispatcher.io) {
            userRepository.observeLegislaturaId().collect { legislaturaId ->
                _homeState.update { it.copy(legislaturaId = legislaturaId) }
            }
        }
        viewModelScope.launch(dispatcher.io) {
            legislaturasRepository.getLegislaturas().collect { legislaturas ->
                _homeState.update { it.copy(legislaturas = legislaturas) }
            }
        }
    }

    /**
     * Writing the row is not enough on its own. Reads react to it, but the term being switched
     * to has no local data yet, so the sync has to run again — and it is the sync that puts the
     * screen in a loading state instead of showing an empty Home as if that were the answer.
     *
     * The flag is set before the sync starts so that its very first emission is already read as
     * a switch. A term that was downloaded before never reaches the network here: the use case
     * finds the data locally and answers Done, which clears the flag on the spot.
     */
    private fun selectLegislatura(legislaturaId: String) {
        val current = _homeState.value.legislaturaId
        if (current == legislaturaId) {
            logger.d("selectLegislatura: already on $legislaturaId, ignoring", TAG)
            return
        }

        viewModelScope.launch(dispatcher.io) {
            switchingLegislatura = true
            _homeState.update { it.copy(legislaturaSync = LegislaturaSyncState.Syncing) }

            userRepository.setLegislatura(legislaturaId)
            analytics.track(
                AnalyticsEvent.LegislaturaChanged(from = current.orEmpty(), to = legislaturaId)
            )
            trySync()
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
                    is SyncUserInformationState.Retry -> trackSyncFinished(false, startedAt)
                    SyncUserInformationState.Initial,
                    SyncUserInformationState.Downloading -> Unit
                }

                val legislaturaSync = legislaturaSyncStateFor(state, switchingLegislatura)

                // Cleared only on success. A switch that failed keeps its own state so the
                // retry button belongs to the term, not to the app as a whole.
                if (state is SyncUserInformationState.Done) switchingLegislatura = false

                _homeState.update { it.copy(syncState = state, legislaturaSync = legislaturaSync) }
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
