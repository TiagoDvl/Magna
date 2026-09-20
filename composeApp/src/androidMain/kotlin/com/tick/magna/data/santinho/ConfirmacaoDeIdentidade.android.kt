package com.tick.magna.data.santinho

import android.app.KeyguardManager
import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/**
 * Whatever the phone already uses to unlock itself.
 *
 * `BIOMETRIC_WEAK or DEVICE_CREDENTIAL` on purpose, and both halves matter. Device credential
 * means somebody with no fingerprint enrolled still gets asked — their PIN is their lock, and a
 * feature that only worked for people with fingerprint sensors would guard the wrong half of
 * the users. Weak rather than strong because the question here is "is this the person who owns
 * this phone", not "unlock a key with this"; the key is unlocked by the keystore either way,
 * and demanding class-3 biometrics would shut out a lot of perfectly ordinary devices.
 *
 * [Confirmacao.INDISPONIVEL] when the phone has no lock screen at all. The caller saves anyway:
 * see the enum for why.
 */
internal class ConfirmacaoDeIdentidade(
    private val context: Context,
) : ConfirmacaoDeIdentidadeInterface {

    override suspend fun confirmar(
        titulo: String,
        descricao: String,
        botao: String,
    ): Confirmacao {
        val activity = context.activity() ?: return Confirmacao.INDISPONIVEL

        if (!vaiConseguirPerguntar()) return Confirmacao.INDISPONIVEL

        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val prompt = BiometricPrompt(
                    activity,
                    ContextCompat.getMainExecutor(context),
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(
                            result: BiometricPrompt.AuthenticationResult,
                        ) {
                            if (continuation.isActive) continuation.resume(Confirmacao.CONFIRMADA)
                        }

                        // A wrong finger is not a refusal — the prompt stays up and asks
                        // again — so only this callback ends the attempt. What it has to
                        // separate is somebody saying no from the phone saying it cannot ask,
                        // because those two lead to opposite outcomes: the first must block
                        // the save, the second must not be allowed to block it forever.
                        override fun onAuthenticationError(code: Int, message: CharSequence) {
                            if (!continuation.isActive) return

                            continuation.resume(
                                if (code in NAO_DEU_PRA_PERGUNTAR) {
                                    Confirmacao.INDISPONIVEL
                                } else {
                                    Confirmacao.RECUSADA
                                },
                            )
                        }
                    },
                )

                val info = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(titulo)
                    .setSubtitle(descricao)
                    .setAllowedAuthenticators(AUTENTICADORES)
                    .build()

                prompt.authenticate(info)
                continuation.invokeOnCancellation { prompt.cancelAuthentication() }
            }
        }
    }

    /**
     * Whether it is worth putting the prompt up at all.
     *
     * `canAuthenticate` is asked first and believed when it says yes. When it says no, the
     * lock screen is asked directly, because on some versions the biometric library reports
     * only what is *enrolled* — a phone with a PIN and no fingerprint can come back as
     * `BIOMETRIC_ERROR_NONE_ENROLLED` while being perfectly able to show a PIN pad.
     *
     * Getting that wrong is not a cosmetic bug: [Confirmacao.INDISPONIVEL] means the note is
     * saved without asking anybody anything, so a false "cannot ask" quietly removes the lock
     * from the feature on exactly the phones whose owners chose a PIN over a fingerprint. If
     * the screen is secured, the prompt goes up and the error callback sorts out the rest.
     */
    private fun vaiConseguirPerguntar(): Boolean {
        val resposta = BiometricManager.from(context).canAuthenticate(AUTENTICADORES)
        if (resposta == BiometricManager.BIOMETRIC_SUCCESS) return true

        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager

        return keyguard?.isDeviceSecure == true
    }

    /**
     * The activity behind whatever Context Koin handed over.
     *
     * `BiometricPrompt` needs a FragmentActivity and the DI graph holds the application
     * context, so this walks the wrappers. Null is a real outcome — a Context with no activity
     * under it cannot show a system dialog — and it is treated as "nothing to ask with".
     */
    private fun Context.activity(): FragmentActivity? {
        var atual: Context? = this

        while (atual is ContextWrapper) {
            if (atual is FragmentActivity) return atual
            atual = atual.baseContext
        }

        return null
    }

    private companion object {
        const val AUTENTICADORES =
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL

        /**
         * The phone could not ask, as opposed to the person having said no.
         *
         * Lockout is deliberately not in here. With device credential allowed the prompt falls
         * back to the PIN rather than giving up, so a lockout that still ends the attempt is
         * somebody walking away from it — which is a no.
         */
        val NAO_DEU_PRA_PERGUNTAR = setOf(
            BiometricPrompt.ERROR_HW_NOT_PRESENT,
            BiometricPrompt.ERROR_HW_UNAVAILABLE,
            BiometricPrompt.ERROR_NO_BIOMETRICS,
            BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
            BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED,
        )
    }
}

/** The Context a composable sits in is the activity's, which is exactly the one needed. */
@Composable
actual fun lembrarConfirmacaoDeIdentidade(): ConfirmacaoDeIdentidadeInterface {
    val context = LocalContext.current

    return remember(context) { ConfirmacaoDeIdentidade(context) }
}
