package com.tick.magna.data.santinho

import androidx.compose.runtime.Composable

/**
 * Turns the note into something the device can store and nothing else can read.
 *
 * **What this can honestly promise, and what it cannot.** The key lives in the platform's own
 * keystore and never reaches this code, so the bytes in the database are useless on any other
 * machine and useless in a backup. It does not protect the note from somebody holding the
 * unlocked phone with the app open — nothing at this layer can, and the screen is built on
 * that assumption rather than against it.
 *
 * [disponivel] is false where the platform has no keystore to put a key in. The feature is not
 * offered there at all: a note the app calls safe while storing it in the clear would be worse
 * than no note.
 */
interface CofreLocalInterface {

    val disponivel: Boolean

    /** Null when the platform cannot, or when the keystore refuses. */
    fun cifrar(texto: String): ByteArray?

    /**
     * Null when the bytes cannot be read: a wrong key, a key the system dropped because the
     * lock screen was removed, or a blob from another install. The caller treats that as no
     * note rather than as an error, because to the person it is the same thing.
     */
    fun decifrar(bytes: ByteArray): String?
}

/** What the platform offers for confirming that the person is the owner of the phone. */
enum class Confirmacao {
    /** They proved it. */
    CONFIRMADA,

    /** They were asked and said no, or failed. Nothing is saved. */
    RECUSADA,

    /**
     * There is nothing to ask with: no fingerprint, no face, no PIN, no pattern.
     *
     * Saving goes ahead. A phone with no lock screen has no secret to guard the note with, and
     * refusing to let somebody write a note because their phone is unlocked would be punishing
     * them for a setting this app does not control.
     */
    INDISPONIVEL,
}

/** Asks the system to confirm the person, with whatever the system has. */
interface ConfirmacaoDeIdentidadeInterface {
    suspend fun confirmar(titulo: String, descricao: String, botao: String): Confirmacao
}

/**
 * The confirmation, resolved where the UI is.
 *
 * Deliberately not in the DI graph. A system prompt needs the activity that is on screen, and
 * the graph holds the application context — asking Koin for it would mean keeping a reference
 * to an activity in a singleton, which is the bug this avoids rather than the design it uses.
 */
@Composable
expect fun lembrarConfirmacaoDeIdentidade(): ConfirmacaoDeIdentidadeInterface
