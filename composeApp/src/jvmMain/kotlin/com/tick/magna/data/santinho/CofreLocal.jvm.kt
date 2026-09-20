package com.tick.magna.data.santinho

import androidx.compose.runtime.Composable

/**
 * Not here.
 *
 * The desktop build is a development target with no secure enclave behind it and no system
 * prompt to confirm who is sitting at the keyboard. A note the app called safe while writing
 * it to a file anyone can read would be worse than no note, so the feature is simply not
 * offered on this platform.
 */
internal class CofreLocal : CofreLocalInterface {
    override val disponivel: Boolean = false
    override fun cifrar(texto: String): ByteArray? = null
    override fun decifrar(bytes: ByteArray): String? = null
}

internal class ConfirmacaoDeIdentidade : ConfirmacaoDeIdentidadeInterface {
    override suspend fun confirmar(titulo: String, descricao: String, botao: String) =
        Confirmacao.INDISPONIVEL
}

@Composable
actual fun lembrarConfirmacaoDeIdentidade(): ConfirmacaoDeIdentidadeInterface =
    ConfirmacaoDeIdentidade()
