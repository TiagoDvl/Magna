package com.tick.magna.data.repository.deputados.result

import com.tick.magna.data.domain.DeputadoDetails

sealed interface DeputadoDetailsResult {

    data object Fetching: DeputadoDetailsResult

    data object Error: DeputadoDetailsResult

    data class Success(val details: DeputadoDetails): DeputadoDetailsResult
}