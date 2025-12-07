package com.tick.magna.data.source.local.dao

import com.tick.magna.DeputadoExpense
import kotlinx.coroutines.flow.Flow

interface DeputadoExpenseDaoInterface {

    fun insertDeputadoExpenses(deputadoExpenses: List<DeputadoExpense>)
    fun getDeputadoExpense(deputadoId: String, legislaturaId: String): Flow<List<DeputadoExpense>>
}
