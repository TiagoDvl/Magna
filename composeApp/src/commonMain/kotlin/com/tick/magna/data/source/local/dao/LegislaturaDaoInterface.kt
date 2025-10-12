package com.tick.magna.data.source.local.dao

import com.tick.magna.Legislatura

interface LegislaturaDaoInterface {
    suspend fun getLegislaturaById(id: String): Legislatura?
    suspend fun getAllLegislaturas(): List<Legislatura>
    suspend fun insertLegislatura(legislatura: Legislatura)
    suspend fun deleteLegislaturaById(id: String)

    suspend fun getFirstLegislatura(): Legislatura?
}