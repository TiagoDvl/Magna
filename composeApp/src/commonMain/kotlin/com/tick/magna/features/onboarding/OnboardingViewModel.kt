package com.tick.magna.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.repository.LegislaturaRepositoryInterface
import com.tick.magna.data.usecases.ConfigureLegislaturaUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val legislaturaRepository: LegislaturaRepositoryInterface,
    private val configureLegislatura: ConfigureLegislaturaUseCase,
    private val dispatcherInterface: DispatcherInterface
): ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    init {
        viewModelScope.launch(dispatcherInterface.io) {
            legislaturaRepository.getAllLegislaturas().collect { result ->
                _state.update { it.copy(legislaturas = result) }
            }
        }
    }

    fun processAction(action: Action) {
        viewModelScope.launch(dispatcherInterface.default) {
            when (action) {
                is Action.PickLegislaturaPeriod -> configureLegislatura(action.legislaturaPeriod)
            }
        }
    }
}
