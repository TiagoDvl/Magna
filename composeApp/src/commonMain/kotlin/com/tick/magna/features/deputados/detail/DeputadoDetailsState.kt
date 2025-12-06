package com.tick.magna.features.deputados.detail

import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.domain.DeputadoDetails
import com.tick.magna.data.domain.DeputadoExpense

data class DeputadoDetailsState(
    val deputado: Deputado? = null,
    val detailsState: DetailsState = DetailsState.Loading,
    val expensesState: ExpensesState = ExpensesState.Loading
)

sealed interface DetailsState {

    data object Loading: DetailsState

    data class Content(val deputadoDetails: DeputadoDetails): DetailsState
}

sealed interface ExpensesState {

    data object Loading: ExpensesState

    data class Content(val expenses: List<DeputadoExpense>): ExpensesState
}

sealed interface DeputadoDetailsSheetState {
    data class Expense(val deputadoExpense: DeputadoExpense): DeputadoDetailsSheetState
}