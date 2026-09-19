package com.tick.magna.features.deputados.details

import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.domain.DeputadoDetails
import com.tick.magna.data.domain.DeputadoExpense
import com.tick.magna.data.domain.ImportacaoVotos
import com.tick.magna.data.domain.VotoDeputado

data class DeputadoDetailsState(
    val deputado: Deputado? = null,
    val detailsState: DetailsState = DetailsState.Loading,
    val expensesState: ExpensesState = ExpensesState.Loading,
    val votosState: VotosState = VotosState.Loading,
    val selectedTab: DeputadoTab = DeputadoTab.DESPESAS,
    val importacao: ImportacaoState = ImportacaoState.Escondida,
)

enum class DeputadoTab { DESPESAS, VOTOS }

/**
 * The optional full-year download, which is the only thing in this app that moves megabytes.
 *
 * Its states are the product rules made visible: nothing is offered for a past term, the size
 * is on the button before anything transfers, what is stored declares how complete it is, and
 * the refresh only appears when a `HEAD` found something newer.
 */
sealed interface ImportacaoState {

    /** Nothing to offer: a past term, or the size could not be read. */
    data object Escondida : ImportacaoState

    data class Disponivel(val bytes: Long) : ImportacaoState

    data class Baixando(val progresso: Float) : ImportacaoState

    data class Completa(
        val completoAte: String?,
        val votos: Long,
        val desatualizada: Boolean,
        val bytes: Long,
    ) : ImportacaoState

    /** The download failed and nothing was written, which is worth saying out loud. */
    data class Falhou(val bytes: Long) : ImportacaoState
}

internal fun importacaoStateFor(importacao: ImportacaoVotos): ImportacaoState = when (importacao) {
    ImportacaoVotos.Indisponivel -> ImportacaoState.Escondida
    is ImportacaoVotos.Disponivel -> ImportacaoState.Disponivel(importacao.bytes)
    is ImportacaoVotos.Completa -> ImportacaoState.Completa(
        completoAte = importacao.completoAte,
        votos = importacao.votos,
        desatualizada = importacao.desatualizada,
        bytes = importacao.bytes,
    )
}

/**
 * Megabytes with one decimal, because this number is a promise.
 *
 * Rounded down deliberately: the download turning out smaller than announced is fine, larger
 * is not.
 */
internal fun formatarBytes(bytes: Long): String {
    val decimos = bytes * 10 / (1024 * 1024)

    return "${decimos / 10},${decimos % 10} MB"
}

sealed interface DetailsState {

    data object Loading: DetailsState

    data object Error: DetailsState

    data class Content(val deputadoDetails: DeputadoDetails): DetailsState
}

sealed interface ExpensesState {

    data object Loading: ExpensesState

    data object Error: ExpensesState

    /** The deputado has no expenses for the period, which is different from a failure. */
    data object Empty: ExpensesState

    data class Content(val expenses: List<DeputadoExpense>): ExpensesState
}

/**
 * How this deputado voted, which the app could not answer until there was a local index.
 *
 * [Empty] is not a rare branch here, it is the common one. Only 2% of what the Camara registers
 * is nominal — 152 votacoes out of 7360 in 2026 — and the window the app syncs is one quarter,
 * so a deputado with nothing to show is an ordinary outcome rather than a failure. Saying so
 * matters more than usual: a silent empty list reads as "did not vote", and what it means is
 * "the Camara recorded no individual votes here".
 */
sealed interface VotosState {

    data object Loading : VotosState

    data object Error : VotosState

    data object Empty : VotosState

    /** Never constructed with an empty list; that is [Empty]. */
    data class Content(val votos: List<VotoDeputado>) : VotosState {
        val sim: Int get() = votos.count { it.voto == "Sim" }
        val nao: Int get() = votos.count { it.voto == "Não" }
        val outros: Int get() = votos.size - sim - nao
    }
}

internal fun votosStateFor(result: Result<List<VotoDeputado>>): VotosState {
    val votos = result.getOrElse { return VotosState.Error }

    return if (votos.isEmpty()) VotosState.Empty else VotosState.Content(votos)
}

sealed interface DeputadoDetailsSheetState {
    data class Expense(val deputadoExpense: DeputadoExpense): DeputadoDetailsSheetState
}