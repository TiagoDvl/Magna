package com.tick.magna.features.comissoes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.orgaos.OrgaosRepositoryInterface
import com.tick.magna.features.comissoes.domain.ComissaoPermanente
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

class ComissoesPermanentesViewModel(
    orgaosRepository: OrgaosRepositoryInterface,
    dispatcherInterface: DispatcherInterface,
    loggerInterface: AppLoggerInterface,
) : ViewModel() {

    val state: StateFlow<List<ComissaoPermanente>> = orgaosRepository
        .getComissoesPermanentes()
        .map { listaOrgaos ->
            loggerInterface.d("1 - map -> $listaOrgaos")
            listaOrgaos.mapNotNull { orgao ->
                loggerInterface.d("2 - mapNotNull -> $orgao")
                if (orgao.nome != null && orgao.nomeResumido != null) {
                    ComissaoPermanente(
                        nomeResumido = orgao.nomeResumido,
                        nome = orgao.nome
                    )
                } else {
                    null
                }
            }
        }
        .onEach {
            loggerInterface.d("ComissoesPermanentesViewModel -> $it")
        }
        .flowOn(dispatcherInterface.io)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )
}