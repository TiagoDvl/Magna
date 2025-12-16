package com.tick.magna.features.proposicoes.component

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.repository.ProposicoesRepositoryInterface
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class RecentPECsViewModel(
    proposicoesRepository: ProposicoesRepositoryInterface,
    dispatcherInterface: DispatcherInterface
) : ViewModel() {


    val state = proposicoesRepository.observeRecentProposicoes()
        .map {
            RecentPECsState(proposicoes = it)
        }
        .flowOn(dispatcherInterface.io)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = RecentPECsState()
        )
}
