package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.response.LegislaturaResponse
import com.tick.magna.data.source.remote.response.LegislaturasResponse

internal interface LegislaturaApiInterface {
    suspend fun getLegislaturas(date: String): LegislaturasResponse

    suspend fun getLegislaturaById(id: Int): LegislaturaResponse
}
