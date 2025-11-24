package com.tick.magna.features.home

import androidx.lifecycle.ViewModel
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.usecases.CheckUserConfigurationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel(
    dispatcher: DispatcherInterface,
    checkUserConfiguration: CheckUserConfigurationUseCase,
): ViewModel() {

    private val _homeState = MutableStateFlow(HomeState)
    val homeState: StateFlow<HomeState> = _homeState.asStateFlow()

}
