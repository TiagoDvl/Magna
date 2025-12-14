package com.tick.magna.data.repository

interface ProposicoesRepositoryInterface {

    suspend fun syncSiglaTipos(): Boolean
}