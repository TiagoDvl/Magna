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
    loggerInterface: AppLoggerInterface,
) : ViewModel() {

    val state: StateFlow<List<ComissaoPermanente>> = orgaosRepository
        .getComissoesPermanentes()
        .map { listaOrgaos ->
            listaOrgaos.mapNotNull { orgao ->
                if (orgao.nome != null && orgao.nomeResumido != null) {
                    ComissaoPermanente(
                        comissaoPermanenteId = orgao.id,
                        nomeResumido = orgao.nomeResumido,
                        nome = orgao.nome
                    )
                } else {
                    null
                }
            }
        }
        .flowOn(dispatcherInterface.io)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )
}