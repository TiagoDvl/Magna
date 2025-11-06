package com.tick.magna.data.repository.result

import com.tick.magna.data.domain.DeputadoDetails

sealed interface DeputadoDetailsResult {

    data object Fetching: DeputadoDetailsResult

    data class Success(val details: DeputadoDetails): DeputadoDetailsResult
}
