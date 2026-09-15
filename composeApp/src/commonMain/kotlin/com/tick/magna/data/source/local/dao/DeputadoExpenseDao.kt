package com.tick.magna.data.source.local.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tick.magna.DeputadoExpense
import com.tick.magna.DeputadoExpenseQueries
import com.tick.magna.MagnaDatabase
import com.tick.magna.data.dispatcher.DispatcherInterface
import kotlinx.coroutines.flow.Flow

class DeputadoExpenseDao(
    db: MagnaDatabase,
    private val dispatcherInterface: DispatcherInterface
): DeputadoExpenseDaoInterface {

    private val queries: DeputadoExpenseQueries = db.deputadoExpenseQueries

    /**
     * Upserts on the document key, so reopening a deputado refreshes the rows instead of
     * appending a second copy of the whole list.
     */
    override fun insertDeputadoExpenses(deputadoExpenses: List<DeputadoExpense>) {
        queries.transaction {
            deputadoExpenses.forEach {
                queries.insertExpense(
                    deputadoId = it.deputadoId,
                    legislaturaId = it.legislaturaId,
                    codDocumento = it.codDocumento,
                    parcela = it.parcela,
                    year = it.year,
                    month = it.month,
                    despesaType = it.despesaType,
                    documentDate = it.documentDate,
                    documentNumber = it.documentNumber,
                    documentValue = it.documentValue,
                    documentUrl = it.documentUrl,
                    fornecedorName = it.fornecedorName,
                    cnpjCpf = it.cnpjCpf,
                )
            }
        }
    }

    override fun getDeputadoExpense(deputadoId: String, legislaturaId: String): Flow<List<DeputadoExpense>> {
        return queries.getExpensesByDeputado(deputadoId = deputadoId, legislaturaId = legislaturaId)
            .asFlow()
            .mapToList(dispatcherInterface.io)
    }
}
