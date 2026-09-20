package com.tick.magna.features.santinho

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.santinho.CargoDaUrna
import com.tick.magna.data.santinho.Santinho
import com.tick.magna.data.santinho.SantinhoRepositoryInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * The note being edited, and nothing about it that leaves the device.
 *
 * There is no analytics call in this file, deliberately and permanently. Everywhere else in the
 * app a screen reports that it was opened; this one does not, because "opened the santinho" is
 * already a fact about somebody's intention to vote, and the only number of such facts worth
 * collecting is none.
 */
class SantinhoViewModel(
    private val repository: SantinhoRepositoryInterface,
) : ViewModel() {

    private val _state = MutableStateFlow(SantinhoState(disponivel = repository.disponivel))
    val state: StateFlow<SantinhoState> = _state.asStateFlow()

    init {
        // Observed, not read once. Two ViewModels exist at the same time — the Home's banner
        // has one and the editing screen another — and a one-shot read left the banner saying
        // what had been true when the Home was first composed.
        viewModelScope.launch {
            repository.observar().collect { doBanco ->
                _state.update { atual ->
                    atual.copy(
                        // Only when nothing is half-typed. A write from elsewhere must not
                        // take the digits out from under somebody's thumb.
                        santinho = if (atual.carregando || !atual.alterado) doBanco else atual.santinho,
                        guardado = doBanco,
                        carregando = false,
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.bannerDispensado.collect { dispensado ->
                _state.update { it.copy(bannerVisivel = !dispensado) }
            }
        }
    }

    /** Spent, for as long as this ViewModel lives — which is as long as the Home does. */
    fun onBrilhoMostrado() {
        _state.update { it.copy(brilhoPendente = false) }
    }

    fun onBannerDispensado() {
        viewModelScope.launch { repository.dispensarBanner() }
    }

    fun onCampoAlterado(cargo: CargoDaUrna, valor: String) {
        _state.update { atual ->
            atual.copy(santinho = atual.santinho.com(cargo, valor), aviso = null)
        }
    }

    /**
     * Revealing is a decision the system has already confirmed; hiding is not.
     *
     * Asymmetric on purpose. Making somebody prove who they are to *stop* showing their own
     * numbers would be a lock on the wrong side of the door, and the useful version of that
     * gesture is the one that works while a queue is forming behind them.
     */
    fun onVisibilidadeAlterada(revelado: Boolean) {
        _state.update { it.copy(revelado = revelado) }
    }

    /**
     * Called only after the system has confirmed the person, or said it had no way to.
     *
     * The confirmation happens in the UI because it is a system dialog and needs the activity;
     * what this owns is the rule that nothing is written before that call comes back.
     */
    fun onConfirmado() {
        val paraGuardar = _state.value.santinho

        viewModelScope.launch {
            val deuCerto = repository.guardar(paraGuardar, agora())

            // `guardado` is not set here: the write lands in the table and the flow above
            // reports it, which is the only path that both this screen and the banner see.
            _state.update {
                it.copy(
                    aviso = if (deuCerto) AvisoDoSantinho.GUARDADO else AvisoDoSantinho.FALHOU,
                )
            }
        }
    }

    fun onRecusado() {
        _state.update { it.copy(aviso = AvisoDoSantinho.RECUSADO) }
    }

    fun onMostrarRecusado() {
        _state.update { it.copy(aviso = AvisoDoSantinho.NAO_MOSTRADO) }
    }

    fun onApagar() {
        viewModelScope.launch {
            repository.apagar()

            // The editing buffer, which the flow will not clear: an emptied note and a
            // half-typed one are the same shape, so the flow leaves the buffer alone by
            // design and this is the one place that is a deliberate discard.
            _state.update { it.copy(santinho = Santinho(), aviso = null) }
        }
    }

    fun onAvisoVisto() {
        _state.update { it.copy(aviso = null) }
    }

    /**
     * The day, not the instant.
     *
     * A timestamp to the second would say which minute somebody sat down to decide their vote,
     * and the only thing the screen needs to say is which day the note is from.
     */
    private fun agora(): String =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
}
