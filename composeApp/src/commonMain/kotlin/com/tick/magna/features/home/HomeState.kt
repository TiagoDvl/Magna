package com.tick.magna.features.home

import com.tick.magna.data.usecases.SyncUserInformationState

data class HomeState(
    val syncState: SyncUserInformationState = SyncUserInformationState.Running,
)

sealed interface HomeAction {
    data object RetrySync : HomeAction
}

enum class HomeSheetState {
    RUNNING_SYNC,
    RETRY_SYNC
}