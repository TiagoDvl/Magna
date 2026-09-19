package com.tick.magna.features.home

import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.domain.Legislatura
import com.tick.magna.data.usecases.SyncStep
import com.tick.magna.data.usecases.SyncUserInformationState

data class HomeState(
    val syncState: SyncUserInformationState = SyncUserInformationState.Initial,
    val filteredDeputados: List<Deputado>? = null,
    /** Null until the user row is written, which is why the selector can be absent. */
    val legislaturaId: String? = null,
    val legislaturas: List<Legislatura> = emptyList(),
    /** Null whenever no term switch is in flight. See [legislaturaSyncStateFor]. */
    val legislaturaSync: LegislaturaSyncState? = null,
) {

    val selectedLegislatura: Legislatura?
        get() = legislaturas.find { it.id == legislaturaId }

    /**
     * Whether the screen has nothing to show yet and should say so with the whole surface.
     *
     * A cold start has no data of any term, so covering the screen costs nothing. A term switch
     * does: the selector is on that surface, and hiding it is hiding the control the person
     * just used — including the way back to the term that did work.
     */
    val isBlockingSync: Boolean
        get() = syncState !is SyncUserInformationState.Done && legislaturaSync == null
}

/**
 * What a term switch is doing, as opposed to what the sync is doing.
 *
 * They are the same events seen from different places: a failed sync on a cold start is a
 * broken app, and the same failure right after a switch is one term that did not download
 * while everything else still works.
 */
sealed interface LegislaturaSyncState {

    data object Syncing : LegislaturaSyncState

    /**
     * [failedSteps] is empty when the sync gave up before running any step at all.
     */
    data class Incomplete(val failedSteps: Set<SyncStep>) : LegislaturaSyncState {

        /**
         * Nothing arrived, which is a connection problem and reads as one. Listing all five
         * section names would be technically accurate and useless — the person does not have a
         * committee problem, they have no network.
         */
        val isEverythingDown: Boolean
            get() = failedSteps.isEmpty() || failedSteps.containsAll(SyncStep.entries)
    }
}

/**
 * Kept out of the ViewModel so the rule can be read and tested on its own.
 *
 * [switching] is what separates the two readings of the same sync state, and it stays true
 * across a retry: retrying a switch is still a switch, and dropping back to the blocking
 * dialog halfway through would take the selector away at the worst moment.
 */
internal fun legislaturaSyncStateFor(
    syncState: SyncUserInformationState,
    switching: Boolean,
): LegislaturaSyncState? {
    if (!switching) return null

    return when (syncState) {
        SyncUserInformationState.Initial,
        SyncUserInformationState.Downloading -> LegislaturaSyncState.Syncing

        SyncUserInformationState.Done -> null

        is SyncUserInformationState.Retry -> LegislaturaSyncState.Incomplete(syncState.failedSteps)
    }
}

sealed interface HomeAction {
    data object RetrySync : HomeAction
    data class SearchDeputado(val query: String) : HomeAction

    /** Reported only; opening the deputado is the navigation controller's job. */
    data object SearchResultOpened : HomeAction

    data class SelectLegislatura(val legislaturaId: String) : HomeAction
}
