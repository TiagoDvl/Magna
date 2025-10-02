package com.tick.magna.shared.data.di

import com.tick.magna.shared.data.repository.DeputadosRepository
import com.tick.magna.shared.data.source.remote.HttpClientFactory
import com.tick.magna.shared.data.source.remote.api.DeputadosApi

object DataModule {
    private val httpClient by lazy { HttpClientFactory.create() }
    
    val deputadosRepository: DeputadosRepository by lazy {
        DeputadosRepository(DeputadosApi(httpClient))
    }
}