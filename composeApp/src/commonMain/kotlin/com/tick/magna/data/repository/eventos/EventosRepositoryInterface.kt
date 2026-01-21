package com.tick.magna.data.repository.eventos

import com.tick.magna.data.domain.Pauta

interface EventosRepositoryInterface {

    suspend fun getEventoPautas(idEvento: String): List<Pauta>
}