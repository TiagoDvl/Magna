package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.response.LegislaturasResponse

interface LegislaturasApiInterface {

    suspend fun getLegislaturas(): LegislaturasResponse
}
