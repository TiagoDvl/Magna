package com.tick.magna.data.source.local.dao

import com.tick.magna.Legislatura
import kotlinx.coroutines.flow.Flow

interface LegislaturaDaoInterface {
    fun getAllLegislaturas(): Flow<List<Legislatura>>
    fun insertLegislaturas(legislaturas: List<Legislatura>)
}