package com.tick.magna.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.model.Deputado
import com.tick.magna.data.repository.PlenarioRepositoryInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    val plenarioRepository: PlenarioRepositoryInterface,
    val logger: AppLoggerInterface
): ViewModel(), HomeMediator {

    override val deputadosListState = MutableStateFlow<List<Deputado>>(emptyList())

    init {
        logger.i("Vamo começar a usar viewmodel")
        logger.d("plenarioRepository: $plenarioRepository")
        viewModelScope.launch {
            plenarioRepository.getDeputados()
                .onSuccess { deputadosListState.value = it }
                .onFailure { logger.e(message = it.message ?: "", it.cause) }
        }
    }
}