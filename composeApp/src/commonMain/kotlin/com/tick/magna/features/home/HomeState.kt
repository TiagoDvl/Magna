package com.tick.magna.features.home

import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.usecases.SyncUserInformationState

data class HomeState(
    val syncState: SyncUserInformationState = SyncUserInformationState.Initial,
    val filteredDeputados: List<Deputado>? = null
)

sealed interface HomeAction {
    data object RetrySync : HomeAction
    data class SearchDeputado(val query: String) : HomeAction
}

enum class HomeSheetState {
    RUNNING_SYNC,
    RETRY_SYNC
}