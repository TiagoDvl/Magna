package com.tick.magna.data.repository.deputados.result

import com.tick.magna.data.domain.DeputadoExpense

/**
 * Separates the three outcomes the expenses list used to collapse into one.
 *
 * Before this, an empty list meant "still loading", so a deputado with no expenses in the
 * current year showed a spinner forever, and an API failure looked exactly the same.
 */
sealed interface DeputadoExpensesResult {

    data object Fetching : DeputadoExpensesResult

    data object Error : DeputadoExpensesResult

    /** Empty means the deputado genuinely has no expenses for the period. */
    data class Success(val expenses: List<DeputadoExpense>) : DeputadoExpensesResult
}
