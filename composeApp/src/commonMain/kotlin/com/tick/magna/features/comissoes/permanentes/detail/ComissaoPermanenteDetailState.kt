package com.tick.magna.features.comissoes.permanentes.detail

import com.tick.magna.data.domain.CargoComissao
import com.tick.magna.data.domain.MembroComissao
import com.tick.magna.data.domain.Votacao

data class ComissaoPermanenteState(
    /** Held so the title can be the far end of a shared element keyed by the committee. */
    val comissaoPermanenteId: String? = null,
    val comissaoPermanenteNomeResumido: String? = null,
    val selectedTab: ComissaoTab = ComissaoTab.VOTACOES,
    val votacoesState: VotacoesState = VotacoesState.Loading,
    val membrosState: MembrosState = MembrosState.Loading,
    val presidentesState: PresidentesState = PresidentesState.Idle,
)

enum class ComissaoTab { VOTACOES, COMPOSICAO, PRESIDENTES }

/**
 * Four outcomes, because this screen used to have one.
 *
 * It decided what to show with `if (votacoes.isEmpty()) LoadingComponent()`, so a committee
 * with no votes span forever, and so did a failed request — `isError` was written to the state
 * and never read by anything. Neither showed up in practice only because the six committees
 * the app hardcodes all happen to have votes. CASP has none.
 */
sealed interface VotacoesState {
    data object Loading : VotacoesState
    data object Empty : VotacoesState
    data object Error : VotacoesState

    /** Never constructed with an empty list; that is [Empty]. */
    data class Content(val votacoes: List<Votacao>) : VotacoesState {
        val aprovadas: Int get() = votacoes.count { it.aprovacao }
        val rejeitadas: Int get() = votacoes.size - aprovadas
    }
}

/** The same four, for the other half of the screen. */
sealed interface MembrosState {
    data object Loading : MembrosState
    data object Empty : MembrosState
    data object Error : MembrosState

    /** Never constructed with an empty list; that is [Empty]. */
    data class Content(val membros: List<MembroComissao>) : MembrosState {

        /**
         * President and vice-presidents, in the order the Camara numbers them, which is the
         * order the repository already sorted them into.
         */
        val mesa: List<MembroComissao> get() = membros.filter { it.cargo == CargoComissao.MESA }
        val titulares: List<MembroComissao> get() = membros.filter { it.cargo == CargoComissao.TITULAR }
        val suplentes: List<MembroComissao> get() = membros.filter { it.cargo == CargoComissao.SUPLENTE }
    }
}

/**
 * Five outcomes rather than four, because this one is not fetched until it is asked for.
 *
 * The whole mandate is ten requests on the CCJC against two for the current composition, so
 * paying for it on every committee anybody opens would make the screen slower for everyone to
 * answer a question most visits do not ask.
 */
sealed interface PresidentesState {
    /** The tab has not been opened, so nothing has been requested. */
    data object Idle : PresidentesState
    data object Loading : PresidentesState
    data object Empty : PresidentesState
    data object Error : PresidentesState

    /** Never constructed with an empty list; that is [Empty]. */
    data class Content(val presidentes: List<MembroComissao>) : PresidentesState
}

/**
 * Kept out of the ViewModel so the rule can be read and tested on its own, the way the
 * legislature-switch rule is.
 */
internal fun votacoesStateFor(result: Result<List<Votacao>>): VotacoesState {
    val votacoes = result.getOrElse { return VotacoesState.Error }

    return if (votacoes.isEmpty()) VotacoesState.Empty else VotacoesState.Content(votacoes)
}

internal fun membrosStateFor(result: Result<List<MembroComissao>>): MembrosState {
    val membros = result.getOrElse { return MembrosState.Error }

    return if (membros.isEmpty()) MembrosState.Empty else MembrosState.Content(membros)
}

internal fun presidentesStateFor(result: Result<List<MembroComissao>>): PresidentesState {
    val presidentes = result.getOrElse { return PresidentesState.Error }

    return if (presidentes.isEmpty()) {
        PresidentesState.Empty
    } else {
        PresidentesState.Content(presidentes)
    }
}

/**
 * Whether opening the tab should fetch.
 *
 * Idle is the first visit. Error is a retry: the request cost nothing that survived, there is
 * no button offering another attempt, and coming back to the tab is the gesture somebody
 * makes when a screen failed. Loading and a loaded list are left alone.
 */
internal fun shouldLoadPresidentes(state: PresidentesState): Boolean =
    state == PresidentesState.Idle || state == PresidentesState.Error
