package com.tick.magna.data.source.local.dao

import com.tick.magna.DeputadoDetails
import kotlinx.coroutines.flow.Flow

interface DeputadoDetailsDaoInterface {

    suspend fun insertDeputadosDetails(deputadosDetails: List<DeputadoDetails>)
    fun getDeputadoDetails(legislaturaId: String, deputadoId: String): Flow<DeputadoDetails?>
}
