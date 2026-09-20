package com.tick.magna.data.source.local.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.tick.magna.PreferenciaQueries
import com.tick.magna.data.dispatcher.DispatcherInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface PreferenciaDaoInterface {

    /** Emits again whenever the value changes, which is what makes two screens agree. */
    fun observarBooleano(chave: String): Flow<Boolean>

    suspend fun definirBooleano(chave: String, valor: Boolean)
}

internal class PreferenciaDao(
    private val queries: PreferenciaQueries,
    private val dispatcher: DispatcherInterface,
) : PreferenciaDaoInterface {

    override fun observarBooleano(chave: String): Flow<Boolean> =
        queries.getPreferencia(chave)
            .asFlow()
            .mapToOneOrNull(dispatcher.io)
            .map { it == VERDADEIRO }

    override suspend fun definirBooleano(chave: String, valor: Boolean) {
        queries.setPreferencia(chave, if (valor) VERDADEIRO else FALSO)
    }

    private companion object {
        const val VERDADEIRO = "1"
        const val FALSO = "0"
    }
}

/** The keys, in one place, because a typo in a string literal is a setting that silently resets. */
object Preferencias {

    /** Whether the person closed the santinho banner on the Home. */
    const val SANTINHO_BANNER_DISPENSADO = "santinho.banner.dispensado"
}
