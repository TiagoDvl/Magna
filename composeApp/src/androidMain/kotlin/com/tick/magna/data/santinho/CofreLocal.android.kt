package com.tick.magna.data.santinho

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-GCM with a key the app never sees.
 *
 * `setUserAuthenticationRequired` is deliberately **not** set on the key. It would make the
 * keystore itself demand a lock-screen check before every single use, which sounds stricter
 * and is worse here for two reasons: the note would become unreadable — not just unwritable —
 * the moment somebody turned off their lock screen, taking their own note with it; and the
 * prompt would fire on every read, so opening the screen to glance at four digits would cost a
 * fingerprint. The check belongs on the save, where the person is making a decision, and this
 * layer's job is that the bytes on disk are meaningless without this device.
 *
 * GCM and not CBC because GCM authenticates: a blob somebody edited in the database file fails
 * to decrypt instead of decrypting into digits nobody wrote.
 */
internal class CofreLocal : CofreLocalInterface {

    override val disponivel: Boolean = true

    override fun cifrar(texto: String): ByteArray? = try {
        val cipher = Cipher.getInstance(TRANSFORMACAO)
        cipher.init(Cipher.ENCRYPT_MODE, chave())

        // The IV goes in front of the ciphertext rather than in a column of its own: it is not
        // a secret, it must never repeat, and keeping it welded to the bytes it belongs to is
        // what stops the two from ever being paired up wrongly.
        cipher.iv + cipher.doFinal(texto.encodeToByteArray())
    } catch (e: Exception) {
        null
    }

    override fun decifrar(bytes: ByteArray): String? = try {
        if (bytes.size <= TAMANHO_DO_IV) {
            null
        } else {
            val cipher = Cipher.getInstance(TRANSFORMACAO)
            cipher.init(
                Cipher.DECRYPT_MODE,
                chave(),
                GCMParameterSpec(BITS_DA_ETIQUETA, bytes, 0, TAMANHO_DO_IV),
            )

            cipher.doFinal(bytes, TAMANHO_DO_IV, bytes.size - TAMANHO_DO_IV).decodeToString()
        }
    } catch (e: Exception) {
        null
    }

    private fun chave(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val gerador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        gerador.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(BITS_DA_CHAVE)
                .build()
        )

        return gerador.generateKey()
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "magna.santinho.v1"
        const val TRANSFORMACAO = "AES/GCM/NoPadding"
        const val BITS_DA_CHAVE = 256
        const val BITS_DA_ETIQUETA = 128

        /** What the provider generates for GCM, and what this reads back. */
        const val TAMANHO_DO_IV = 12
    }
}
