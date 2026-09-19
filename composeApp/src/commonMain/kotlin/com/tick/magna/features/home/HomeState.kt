package com.tick.magna.features.home

import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.domain.Legislatura
import com.tick.magna.data.usecases.SyncUserInformationState

data class HomeState(
    val syncState: SyncUserInformationState = SyncUserInformationState.Initial,
    val filteredDeputados: List<Deputado>? = null,
    /** Null until the user row is written, which is why the selector can be absent. */
    val legislaturaId: String? = null,
    val legislaturas: List<Legislatura> = emptyList(),
) {

    val selectedLegislatura: Legislatura?
        get() = legislaturas.find { it.id == legislaturaId }
}

sealed interface HomeAction {
    data object RetrySync : HomeAction
    data class SearchDeputado(val query: String) : HomeAction

    /** Reported only; opening the deputado is the navigation controller's job. */
    data object SearchResultOpened : HomeAction

    data class SelectLegislatura(val legislaturaId: String) : HomeAction
}
