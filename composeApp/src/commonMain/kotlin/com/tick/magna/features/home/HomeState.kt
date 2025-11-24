package com.tick.magna.features.home

data object HomeState

sealed interface HomeAction {

}

enum class HomeSheetState {
    ONBOARDING
}