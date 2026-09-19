package com.tick.magna.data.source.local.dao

import com.tick.magna.DeputadoBio

interface DeputadoBioDaoInterface {

    /** Returns only the ids that are stored, so the caller learns what is missing by subtraction. */
    suspend fun getBios(deputadoIds: List<String>): List<DeputadoBio>

    suspend fun insertBios(bios: List<DeputadoBio>)
}
