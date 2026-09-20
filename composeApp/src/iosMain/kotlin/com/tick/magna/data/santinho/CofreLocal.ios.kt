package com.tick.magna.data.santinho

import androidx.compose.runtime.Composable

/**
 * Not yet.
 *
 * The iOS build exists but is not shipped, and the honest answer for a platform whose keystore
 * this app has not been wired to is that the feature is unavailable — not that the note is
 * stored in the clear behind a screen that calls it safe. The Keychain and LocalAuthentication
 * are the right tools when somebody ships this target.
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
