package com.tick.magna.features.plenario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.DeputadosRepositoryInterface
import kotlinx.coroutines.launch

class PlenarioViewModel(
    val plenarioRepository: DeputadosRepositoryInterface,
    val logger: AppLoggerInterface
): ViewModel() {


    init {
        viewModelScope.launch {
        }
    }
}