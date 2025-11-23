package com.tick.magna.features.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.domain.Legislatura
import com.tick.magna.data.repository.LegislaturaRepositoryInterface
import com.tick.magna.data.usecases.ConfigureLegislaturaUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WelcomeState(
    val isLoading: Boolean = true,
    val legislaturas: List<Legislatura> = emptyList()
)

sealed interface WelcomeAction {
    data class ConfigureLegislatura(val id: String): WelcomeAction
}

class WelcomeViewModel(
    private val dispatcherInterface: DispatcherInterface,
    private val legislaturaRepository: LegislaturaRepositoryInterface,
    private val configureLegislatura: ConfigureLegislaturaUseCase,
): ViewModel() {

    private val _state = MutableStateFlow(WelcomeState())
    val state: StateFlow<WelcomeState> = _state.asStateFlow()

    init {
        viewModelScope.launch(dispatcherInterface.io) {
            legislaturaRepository.getAllLegislaturas().collect { result ->
                _state.update {
                    it.copy(
                        isLoading = result.isEmpty(),
                        legislaturas = result
                    )
                }
            }
        }
    }

    fun sendAction(action: WelcomeAction) {
        viewModelScope.launch(dispatcherInterface.default) {
            when (action) {
                is WelcomeAction.ConfigureLegislatura -> handleLegislaturaConfiguration(action.id)
            }
        }
    }

    private fun handleLegislaturaConfiguration(id: String) {
        viewModelScope.launch(dispatcherInterface.io) {
            configureLegislatura(id)
        }
    }
}