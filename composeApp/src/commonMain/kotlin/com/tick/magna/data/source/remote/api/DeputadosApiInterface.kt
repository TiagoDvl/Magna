package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.response.DeputadoByIdResponse
import com.tick.magna.data.source.remote.response.DeputadosResponse
import com.tick.magna.data.source.remote.response.DespesasResponse

interface DeputadosApiInterface {

    suspend fun getDeputados(legislaturaId: String, page: Int): DeputadosResponse

    /**
     * Who held a seat on [data], which is a different question from who belongs to a term.
     *
     * Filtered by idLegislatura the endpoint returns everyone who ever sat — 879 rows for 648
     * people in the 57th, against 513 seats. Filtered by a single day it returns the house as
     * it stood, for any term: 2020-06-01 gives the 56th's 512, with the seats distributed as
     * the constitution sets them.
     *
     * @param data yyyy-MM-dd. See dataDeReferencia for which day a term answers about.
     */
    suspend fun getDeputadosEmExercicio(data: String, page: Int): DeputadosResponse

    suspend fun getDeputadoById(id: String): DeputadoByIdResponse

    suspend fun getDeputadoExpenses(id: String, legislaturaId: String, year: String): DespesasResponse
}
