package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.response.OrgaosResponse

interface OrgaosApiInterface {

    suspend fun getComissoesPermanentes(): OrgaosResponse
}