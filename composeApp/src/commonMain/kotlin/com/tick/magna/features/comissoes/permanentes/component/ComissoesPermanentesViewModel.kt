package com.tick.magna.features.comissoes.permanentes.component

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.orgaos.OrgaosRepositoryInterface
import com.tick.magna.features.comissoes.permanentes.component.domain.ComissaoPermanente
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ComissoesPermanentesViewModel(
    orgaosRepository: OrgaosRepositoryInterface,
    dispatcherInterface: DispatcherInterface,
    private val loggerInterface: AppLoggerInterface,
) : ViewModel() {

    companion object {
        private const val TAG = "ComissoesPermanentesViewModel"

        /**
         * A shortlist, not a cut: the other twenty are one tap away on the full list.
         *
         * The repository answers busiest first, so this takes the ten that matter most in the
         * selected term rather than the six an enum named in 2023 — among which the CCTI has
         * since fallen to twenty-ninth of thirty.
         */
        private const val PREVIEW_COUNT = 10
    }

    val state: StateFlow<List<ComissaoPermanente>> = orgaosRepository
        .getComissoesPermanentes()
        .map { orgaos ->
            orgaos.toComissoesPermanentes()
                .take(PREVIEW_COUNT)
                .also { comissoes -> loggerInterface.d("comissoesPermanentes: ${comissoes.size} loaded", TAG) }
        }
        .flowOn(dispatcherInterface.io)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )
}
