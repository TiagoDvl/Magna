package com.tick.magna.data.repository

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * The one shape for "loading, failed, or here it is".
 *
 * Replaces four hand-rolled variants that each invented their own signal flows and
 * combined them by hand.
 */
sealed interface Resource<out T> {

    data object Loading : Resource<Nothing>

    data class Error(val cause: Throwable? = null) : Resource<Nothing>

    /** [isRefreshing] is true while cached data is on screen and a request is still running. */
    data class Content<out T>(val data: T, val isRefreshing: Boolean = false) : Resource<T>
}

/**
 * Cache-backed record: serves what is stored, refreshes from the network, and keeps
 * following the cache.
 *
 * The refresh runs inside the flow, so it is cancelled when the collector goes away.
 * Repositories used to launch it into a scope that lived as long as the app, which meant
 * leaving a screen cancelled nothing and a few taps could queue up dozens of requests.
 */
internal fun <T : Any> cachedRecord(
    cache: Flow<T?>,
    refresh: suspend () -> Unit,
): Flow<Resource<T>> = resourceFlow(cache, refresh) { value, refreshState ->
    when {
        value != null -> Resource.Content(value, isRefreshing = refreshState is RefreshState.InFlight)
        refreshState is RefreshState.Failed -> Resource.Error(refreshState.cause)
        // The request succeeded and still nothing was stored: there is nothing to show.
        refreshState is RefreshState.Done -> Resource.Error()
        else -> Resource.Loading
    }
}

/**
 * Cache-backed list. An empty list after a successful refresh is a real answer, not a
 * failure: a deputado can simply have no expenses this year.
 */
internal fun <T> cachedList(
    cache: Flow<List<T>>,
    refresh: suspend () -> Unit,
): Flow<Resource<List<T>>> = resourceFlow(cache, refresh) { items, refreshState ->
    when {
        // Cached rows beat a failed request: last week's data is better than an error
        // screen because the API happens to be down right now.
        items.isNotEmpty() -> Resource.Content(items, isRefreshing = refreshState is RefreshState.InFlight)
        refreshState is RefreshState.Failed -> Resource.Error(refreshState.cause)
        refreshState is RefreshState.Done -> Resource.Content(items)
        else -> Resource.Loading
    }
}

/**
 * Network-only resource, for the screens with nothing cached behind them.
 *
 * [fetch] runs inside the flow, so it is cancelled with the collector, and cancellation is
 * rethrown rather than turned into an error state.
 */
internal fun <T> networkResource(fetch: suspend () -> T): Flow<Resource<T>> = flow {
    emit(Resource.Loading)

    val resource = try {
        Resource.Content(fetch())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        Resource.Error(error)
    }

    emit(resource)
}

private fun <T, R> resourceFlow(
    cache: Flow<T>,
    refresh: suspend () -> Unit,
    toResource: (T, RefreshState) -> Resource<R>,
): Flow<Resource<R>> = channelFlow {
    val refreshState = MutableStateFlow<RefreshState>(RefreshState.InFlight)

    launch {
        refreshState.value = try {
            refresh()
            RefreshState.Done
        } catch (cancellation: CancellationException) {
            // Never report the collector going away as a failure.
            throw cancellation
        } catch (error: Throwable) {
            RefreshState.Failed(error)
        }
    }

    // Collecting here keeps the flow open for as long as the collector wants it, and the
    // refresh above is a child of this scope, so it dies with it.
    cache.combine(refreshState, toResource).collect { resource -> send(resource) }
}

private sealed interface RefreshState {
    data object InFlight : RefreshState
    data object Done : RefreshState
    data class Failed(val cause: Throwable) : RefreshState
}

/** Maps the content of a [Resource] while leaving loading and failure untouched. */
internal fun <T, R> Flow<Resource<T>>.mapContent(transform: (T) -> R): Flow<Resource<R>> =
    map { resource ->
        when (resource) {
            is Resource.Content -> Resource.Content(transform(resource.data), resource.isRefreshing)
            is Resource.Error -> resource
            Resource.Loading -> Resource.Loading
        }
    }
